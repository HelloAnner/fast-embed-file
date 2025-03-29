package com.anner.embed.controller;

import com.anner.embed.LLM;
import com.anner.embed.exception.RagErrorCode;
import com.anner.embed.exception.RagException;
import com.anner.embed.model.ApiResponse;
import com.anner.embed.model.EmbeddingConfig;
import com.anner.embed.model.Task;
import com.anner.embed.model.Task.TaskStatus;
import com.anner.embed.service.EmbeddingService;
import com.anner.embed.service.TaskService;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@RestController
@RequestMapping("/api/embedding")
@RequiredArgsConstructor
public class EmbeddingController {
    private final EmbeddingService embeddingService;
    private final TaskService taskService;

    @PostMapping("/process")
    public ApiResponse<Map<String, String>> processEmbedding(
            @RequestParam("file") MultipartFile file,
            @RequestParam("modelType") String modelType,
            @RequestParam("baseUrl") String baseUrl,
            @RequestParam("apiKey") String apiKey,
            @RequestParam("maxTokensPerChunk") Integer maxTokensPerChunk,
            @RequestParam("overlapTokens") Integer overlapTokens) {
        try {
            if (maxTokensPerChunk == null || maxTokensPerChunk <= 0) {
                return ApiResponse.error(RagErrorCode.INVALID_PARAMETER.getCode(),
                        RagErrorCode.INVALID_PARAMETER.getMessage(),
                        "maxTokensPerChunk必须大于0，当前值：" + maxTokensPerChunk);
            }
            if (overlapTokens == null || overlapTokens < 0) {
                return ApiResponse.error(RagErrorCode.INVALID_PARAMETER.getCode(),
                        RagErrorCode.INVALID_PARAMETER.getMessage(),
                        "overlapTokens不能小于0，当前值：" + overlapTokens);
            }

            // 检查文件类型
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || (!originalFilename.endsWith(".zip") && !originalFilename.endsWith(".tar")
                    && !originalFilename.endsWith(".gz"))) {
                return ApiResponse.error(
                        "1005", // INVALID_FILE_TYPE
                        "不支持的文件类型",
                        String.format("仅支持zip、tar、gz格式的压缩文件，当前文件：%s。\n请确保上传的是正确的压缩文件格式。", originalFilename));
            }

            // 检查文件大小
            if (file.getSize() > 100 * 1024 * 1024) { // 100MB
                return ApiResponse.error(
                        "1003", // FILE_TOO_LARGE
                        "文件大小超过限制",
                        String.format("文件大小不能超过100MB，当前大小：%dMB。\n请压缩文件后重试。", file.getSize() / 1024 / 1024));
            }

            // 检查API配置
            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                return ApiResponse.error(
                        "4003", // API_ERROR
                        "API配置错误",
                        "API地址不能为空，请检查配置。");
            }

            if (apiKey == null || apiKey.trim().isEmpty()) {
                return ApiResponse.error(
                        "4004", // INVALID_API_KEY
                        "API密钥错误",
                        "API密钥不能为空，请检查配置。");
            }

            EmbeddingConfig config = new EmbeddingConfig();
            config.setFile(file);
            config.setModelType(modelType);
            config.setBaseUrl(baseUrl);
            config.setApiKey(apiKey);
            config.setMaxTokensPerChunk(maxTokensPerChunk);
            config.setOverlapTokens(overlapTokens);

            // 创建任务并获取任务ID
            Task task = taskService.createTask(file.getOriginalFilename(), modelType);
            String taskId = task.getId();

            // 异步处理向量化
            embeddingService.processEmbedding(taskId, config);

            // 返回任务ID
            return ApiResponse.success(Map.of("taskId", taskId));
        } catch (RagException e) {
            log.error("文件处理失败: {}", e.getMessage(), e);
            String errorMessage = e.getMessage();
            String detailMessage = errorMessage;

            if (errorMessage.contains("API调用错误")) {
                detailMessage = String.format("%s\n\n请检查：\n1. API地址是否正确\n2. API密钥是否有效\n3. 是否已开通模型使用权限\n4. 模型调用额度是否充足",
                        errorMessage);
            } else if (errorMessage.contains("压缩包中未找到可处理的文件")) {
                detailMessage = String.format(
                        "%s\n\n请检查：\n1. 压缩包中是否包含json、md或txt格式的文件\n2. 文件内容是否有效\n3. 文件编码是否正确（建议使用UTF-8编码）\n4. 压缩包是否完整且可正常解压",
                        errorMessage);
            } else if (errorMessage.contains("文件解压失败")) {
                detailMessage = String.format(
                        "%s\n\n可能的原因：\n1. 压缩包格式不正确或已损坏\n2. 压缩包使用了不支持的压缩算法\n3. 压缩包可能被加密\n\n建议：\n1. 使用标准的zip、tar或gz格式重新压缩\n2. 确保压缩包未加密且完整",
                        errorMessage);
            } else if (errorMessage.contains("向量化处理失败")) {
                detailMessage = String.format(
                        "%s\n\n可能的原因：\n1. API服务异常或超时\n2. 文件内容格式不正确\n3. 模型调用失败\n\n建议：\n1. 检查网络连接\n2. 确认文件内容格式正确\n3. 稍后重试",
                        errorMessage);
            }

            return ApiResponse.error(
                    e.getErrorCode(),
                    e.getErrorMessage(),
                    detailMessage);
        } catch (Exception e) {
            log.error("系统错误", e);
            String errorMessage = String.format("处理过程中发生未知错误：%s", e.getMessage());
            String detailMessage = String.format("%s\n\n建议：\n1. 检查网络连接是否稳定\n2. 确认文件格式是否正确\n3. 稍后重试\n4. 如果问题持续存在，请联系管理员",
                    errorMessage);

            return ApiResponse.error(
                    RagErrorCode.SYSTEM_ERROR.getCode(),
                    RagErrorCode.SYSTEM_ERROR.getMessage(),
                    detailMessage);
        }
    }

    @GetMapping("/progress")
    public ApiResponse<Map<String, Double>> getProgress() {
        try {
            double progress = embeddingService.getProgress();
            return ApiResponse.success(Map.of("progress", progress));
        } catch (RagException e) {
            log.error("获取进度失败: {}", e.getMessage(), e);
            return ApiResponse.error(
                    e.getErrorCode(),
                    e.getErrorMessage(),
                    e.getMessage());
        } catch (Exception e) {
            log.error("获取进度失败", e);
            return ApiResponse.error(
                    RagErrorCode.SYSTEM_ERROR.getCode(),
                    RagErrorCode.SYSTEM_ERROR.getMessage(),
                    "获取进度时发生未知错误：" + e.getMessage());
        }
    }

    @GetMapping("/download/{taskId}")
    public ResponseEntity<Resource> downloadVectorFile(@PathVariable String taskId) {
        try {
            log.debug("开始处理向量文件下载请求，taskId: {}", taskId);
            Task task = taskService.getTask(taskId);

            if (task == null) {
                throw new RagException(RagErrorCode.TASK_NOT_FOUND, "任务不存在，taskId: " + taskId);
            }

            if (task.getStatus() != TaskStatus.COMPLETED) {
                throw new RagException(RagErrorCode.FILE_NOT_FOUND,
                        String.format("任务尚未完成，无法下载文件。当前状态：%s，taskId: %s",
                                task.getStatus(), taskId));
            }

            File vectorFile = new File(task.getVectorFilePath());
            if (!vectorFile.exists() || !vectorFile.canRead()) {
                throw new RagException(RagErrorCode.FILE_NOT_FOUND,
                        String.format("向量化文件不存在或不可读。文件路径：%s，taskId: %s",
                                vectorFile.getAbsolutePath(), taskId));
            }

            Resource resource = new FileSystemResource(vectorFile);
            String filename = task.getFileName().replaceAll("[^a-zA-Z0-9.-]", "_") + "_vectors.json";

            log.debug("准备下载文件：{}，大小：{} bytes", filename, vectorFile.length());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (RagException e) {
            log.error("下载文件失败: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("下载文件失败", e);
            throw new RagException(RagErrorCode.FILE_DOWNLOAD_ERROR,
                    String.format("下载文件时发生未知错误：%s，taskId: %s", e.getMessage(), taskId));
        }
    }

    @PostMapping("/test")
    public ApiResponse<Map<String, Object>> testConfig(
            @RequestParam("file") MultipartFile file,
            @RequestParam("modelType") String modelType,
            @RequestParam("baseUrl") String baseUrl,
            @RequestParam("apiKey") String apiKey) {
        try {
            // 1. 测试 API 连接
            EmbeddingModel model = LLM.doubaoLLMEmbedding(modelType, baseUrl, apiKey);
            String testText = "测试文本";
            model.embed(testText);

            // 2. 检查文件内容
            if (!Objects.requireNonNull(file.getOriginalFilename()).toLowerCase().endsWith(".zip")) {
                throw new RagException(RagErrorCode.INVALID_FILE_TYPE, "只支持ZIP格式的文件");
            }

            // 3. 检查ZIP文件内容
            try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
                ZipEntry entry;
                boolean hasMdFile = false;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().toLowerCase().endsWith(".md")) {
                        hasMdFile = true;
                        break;
                    }
                }
                if (!hasMdFile) {
                    throw new RagException(RagErrorCode.INVALID_FILE_CONTENT, "ZIP文件中未找到Markdown文件");
                }
            }

            return ApiResponse.success(Map.of(
                    "message", "配置测试成功",
                    "hasMdFile", true,
                    "apiTested", true));
        } catch (Exception e) {
            log.error("配置测试失败", e);
            if (e instanceof RagException) {
                throw (RagException) e;
            }
            throw new RagException(RagErrorCode.CONFIG_TEST_FAILED, e.getMessage());
        }
    }
}