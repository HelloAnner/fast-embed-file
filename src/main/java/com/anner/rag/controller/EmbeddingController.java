package com.anner.rag.controller;

import java.io.File;
import java.util.Map;

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

import com.anner.rag.exception.RagErrorCode;
import com.anner.rag.exception.RagException;
import com.anner.rag.model.ApiResponse;
import com.anner.rag.model.EmbeddingConfig;
import com.anner.rag.model.Task;
import com.anner.rag.model.Task.TaskStatus;
import com.anner.rag.service.EmbeddingService;
import com.anner.rag.service.TaskService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
                throw new RagException(RagErrorCode.INVALID_PARAMETER, "maxTokensPerChunk必须大于0");
            }
            if (overlapTokens == null || overlapTokens < 0) {
                throw new RagException(RagErrorCode.INVALID_PARAMETER, "overlapTokens不能小于0");
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
            embeddingService.processEmbedding(taskId,config);

            // 返回任务ID
            return ApiResponse.success(Map.of("taskId", taskId));
        } catch (Exception e) {
            log.error("文件处理失败", e);
            if (e instanceof RagException) {
                throw (RagException) e;
            }
            throw new RagException(RagErrorCode.FILE_PROCESS_FAILED, e);
        }
    }

    @GetMapping("/progress")
    public ApiResponse<Map<String, Double>> getProgress() {
        try {
            double progress = embeddingService.getProgress();
            return ApiResponse.success(Map.of("progress", progress));
        } catch (Exception e) {
            log.error("获取进度失败", e);
            if (e instanceof RagException) {
                throw (RagException) e;
            }
            throw new RagException(RagErrorCode.SYSTEM_ERROR, e);
        }
    }

    @GetMapping("/download/{taskId}")
    public ResponseEntity<Resource> downloadVectorFile(@PathVariable String taskId) {
        try {
            log.debug("开始处理向量文件下载请求，taskId: {}", taskId);
            Task task = taskService.getTask(taskId);

            if (task == null) {
                throw new RagException(RagErrorCode.TASK_NOT_FOUND, "任务不存在");
            }

            if (task.getStatus() != TaskStatus.COMPLETED) {
                throw new RagException(RagErrorCode.FILE_NOT_FOUND, "任务尚未完成，无法下载文件");
            }

            File vectorFile = new File(task.getVectorFilePath());
            if (!vectorFile.exists() || !vectorFile.canRead()) {
                log.error("向量文件不存在或不可读：{}", vectorFile.getAbsolutePath());
                throw new RagException(RagErrorCode.FILE_NOT_FOUND, "向量化文件不存在或不可读");
            }

            Resource resource = new FileSystemResource(vectorFile);
            String filename = task.getFileName().replaceAll("[^a-zA-Z0-9.-]", "_") + "_vectors.json";

            log.debug("准备下载文件：{}，大小：{} bytes", filename, vectorFile.length());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("下载文件失败", e);
            if (e instanceof RagException) {
                throw (RagException) e;
            }
            throw new RagException(RagErrorCode.FILE_DOWNLOAD_ERROR, e);
        }
    }
}