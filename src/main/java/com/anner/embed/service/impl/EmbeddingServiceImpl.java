package com.anner.embed.service.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.anner.embed.LLM;
import com.anner.embed.exception.RagErrorCode;
import com.anner.embed.exception.RagException;
import com.anner.embed.model.EmbeddingConfig;
import com.anner.embed.model.Task;
import com.anner.embed.service.EmbeddingService;
import com.anner.embed.service.TaskService;
import com.anner.embed.util.FileProcessor;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmbeddingServiceImpl implements EmbeddingService {
    private final TaskService taskService;
    private static final String VECTOR_DIR = "vectors";
    private static final String UPLOAD_DIR = "upload_files";

    public EmbeddingServiceImpl(TaskService taskService) {
        this.taskService = taskService;
        try {
            // 确保向量文件目录和上传文件目录存在
            Files.createDirectories(Paths.get(VECTOR_DIR).toAbsolutePath());
            Files.createDirectories(Paths.get(UPLOAD_DIR).toAbsolutePath());
        } catch (IOException e) {
            log.error("创建目录失败", e);
            throw new RagException(RagErrorCode.DIRECTORY_CREATE_FAILED, e);
        }
    }

    @Override
    public CompletableFuture<Void> processEmbedding(String taskId,EmbeddingConfig config) {
        try {
            validateConfig(config);

            // 生成唯一的工作目录
            File workDir = new File(UPLOAD_DIR, taskId);
            createDirectory(workDir);

            // 保存上传的压缩文件
            File compressedFile = saveUploadedFile(config, workDir);

            // 创建解压目录
            File extractDir = new File(workDir, "extracted");
            createDirectory(extractDir);

            // 解压文件
            extractArchive(compressedFile, extractDir);

            // 删除压缩文件
            deleteFile(compressedFile);

            // 获取解压目录的绝对路径，用于后续异步处理
            final String extractDirPath = extractDir.getAbsolutePath();

            // 异步处理向量化
            return CompletableFuture.runAsync(() -> {
                try {
                    processExtractedFiles(taskId, config, extractDirPath, taskId);
                } catch (Exception e) {
                    log.error("向量化处理失败", e);
                    taskService.failTask(taskId, e.getMessage());
                    throw new RagException(RagErrorCode.VECTORIZATION_FAILED, e);
                }
            });
        } catch (RagException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件处理失败", e);
            throw new RagException(RagErrorCode.FILE_PROCESS_FAILED, e);
        }
    }

    private void validateConfig(EmbeddingConfig config) {
        if (config == null) {
            throw new RagException(RagErrorCode.MISSING_PARAMETER, "配置信息不能为空");
        }
        if (config.getFile() == null) {
            throw new RagException(RagErrorCode.MISSING_PARAMETER, "上传文件不能为空");
        }
        if (config.getMaxTokensPerChunk() <= 0) {
            throw new RagException(RagErrorCode.INVALID_PARAMETER, "maxTokensPerChunk必须大于0");
        }
        if (config.getOverlapTokens() < 0) {
            throw new RagException(RagErrorCode.INVALID_PARAMETER, "overlapTokens不能小于0");
        }
    }

    private void createDirectory(File directory) {
        try {
            Files.createDirectories(directory.toPath());
            log.info("创建目录: {}", directory.getAbsolutePath());
        } catch (IOException e) {
            throw new RagException(RagErrorCode.DIRECTORY_CREATE_FAILED, e);
        }
    }

    private File saveUploadedFile(EmbeddingConfig config, File workDir) {
        try {
            byte[] fileBytes = config.getFile().getBytes();
            String originalFilename = StringUtils
                    .cleanPath(Objects.requireNonNull(config.getFile().getOriginalFilename()));
            File compressedFile = new File(workDir, originalFilename);
            FileUtils.writeByteArrayToFile(compressedFile, fileBytes);
            log.info("压缩文件已保存到: {}", compressedFile.getAbsolutePath());
            return compressedFile;
        } catch (IOException e) {
            throw new RagException(RagErrorCode.FILE_UPLOAD_FAILED, e);
        }
    }

    private void extractArchive(File compressedFile, File extractDir) {
        try (ArchiveInputStream<? extends ArchiveEntry> archiveInputStream = new ArchiveStreamFactory()
                .createArchiveInputStream(new BufferedInputStream(new FileInputStream(compressedFile)))) {
            ArchiveEntry entry;
            while ((entry = archiveInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    File outputFile = new File(extractDir, entry.getName());
                    Files.createDirectories(outputFile.getParentFile().toPath());
                    FileUtils.copyToFile(archiveInputStream, outputFile);
                    log.info("已解压文件: {}", outputFile.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            throw new RagException(RagErrorCode.FILE_EXTRACT_ERROR, e);
        }
    }

    private void deleteFile(File file) {
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            log.warn("删除文件失败: {}", file.getAbsolutePath(), e);
        }
    }

    private void processExtractedFiles(String uuidName, EmbeddingConfig config, String extractDirPath, String taskId) {
        try {
            Task task = taskService.getTask(taskId);
            task.setCurrentStage("正在分析文件并进行文本分段");
            log.info("开始处理文件：{}，任务ID：{}", task.getFileName(), taskId);

            // 处理解压后的文件
            Collection<File> files = FileUtils.listFiles(new File(extractDirPath),
                    new String[] { "json", "md" }, true);
            int totalFiles = files.size();
            log.info("共发现 {} 个文件需要处理", totalFiles);

            List<TextSegment> segments = new ArrayList<>();
            int processedFiles = 0;

            for (File file : files) {
                if (taskService.isTaskCancelled(taskId)) {
                    throw new RagException(RagErrorCode.TASK_CANCELLED, "任务已取消");
                }

                log.debug("正在处理文件：{}", file.getName());
                List<TextSegment> fileSegments = FileProcessor.processFiles(
                        Collections.singletonList(file),
                        config.getMaxTokensPerChunk(),
                        config.getOverlapTokens());
                segments.addAll(fileSegments);

                processedFiles++;
                double segmentProgress = (double) processedFiles / totalFiles * 100;
                task.setSegmentProgress(segmentProgress);
                task.setProgress(segmentProgress * 0.3); // 文本分段占总进度的30%
                task.setUpdateTime(LocalDateTime.now());
                log.debug("文件处理进度：{}/{}，当前文件：{}，生成段落数：{}",
                        processedFiles, totalFiles, file.getName(), fileSegments.size());
            }

            log.info("文本分段完成，共生成 {} 个文本段", segments.size());
            task.setCurrentStage("正在生成文本向量");

            // 执行向量化
            InMemoryEmbeddingStore<TextSegment> embeddingStore = generateEmbeddingStore(config, segments, taskId);

            // 保存向量化的结果
            String vectorFilePath = saveVectorFile(uuidName, embeddingStore, segments.size());
            task.setVectorFilePath(vectorFilePath);

            task.setCurrentStage("处理完成");
            taskService.completeTask(taskId);
            log.info("任务处理完成：{}，向量文件已保存：{}", taskId, vectorFilePath);
        } catch (Exception e) {
            log.error("向量化处理失败：{}", e.getMessage(), e);
            throw new RagException(RagErrorCode.VECTORIZATION_FAILED, e);
        }
    }

    private InMemoryEmbeddingStore<TextSegment> generateEmbeddingStore(EmbeddingConfig config,
            List<TextSegment> segments, String taskId) {
        Task task = taskService.getTask(taskId);
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        EmbeddingModel embeddingModel = LLM.doubaoLLMEmbedding(config.getModelType(), config.getBaseUrl(),
                config.getApiKey());

        int totalSegments = segments.size();
        int processedCount = 0;
        log.info("开始生成向量，共 {} 个文本段", totalSegments);

        for (TextSegment segment : segments) {
            if (taskService.isTaskCancelled(taskId)) {
                throw new RagException(RagErrorCode.TASK_CANCELLED, "任务已取消");
            }

            try {
                log.debug("正在处理第 {}/{} 个文本段，长度：{}", processedCount + 1, totalSegments, segment.text().length());
                Embedding embedding = embeddingModel.embed(segment).content();
                embeddingStore.add(embedding, segment);
                processedCount++;

                // 更新任务进度
                double embeddingProgress = (double) processedCount / totalSegments * 100;
                task.setEmbeddingProgress(embeddingProgress);
                // 向量化占总进度的70%
                task.setProgress(30 + embeddingProgress * 0.7);
                task.setUpdateTime(LocalDateTime.now());

                if (processedCount % 10 == 0 || processedCount == totalSegments) {
                    log.info("向量化进度：{}/{}，完成度：{}%",
                            processedCount, totalSegments, embeddingProgress);
                }
            } catch (Exception e) {
                log.error("处理文本段失败：{}", e.getMessage());
                throw new RagException(RagErrorCode.VECTORIZATION_FAILED, e);
            }
        }

        log.info("向量生成完成，共处理 {} 个文本段", totalSegments);
        return embeddingStore;
    }

    private String saveVectorFile(String uuidName, InMemoryEmbeddingStore<TextSegment> embeddingStore,
            int segmentsSize) {
        try {
            String fileName = uuidName + ".json";
            File vectorFile = new File(VECTOR_DIR, fileName);
            log.debug("准备保存向量文件：{}", vectorFile.getAbsolutePath());
            embeddingStore.serializeToFile(vectorFile.getAbsolutePath());
            log.info("向量化处理完成，共处理 {} 个文本段，保存到文件：{}", segmentsSize, vectorFile.getAbsolutePath());

            // 验证文件是否成功保存
            if (!vectorFile.exists()) {
                log.error("向量文件保存失败：文件不存在 {}", vectorFile.getAbsolutePath());
                throw new RagException(RagErrorCode.FILE_WRITE_ERROR, "向量文件保存失败");
            }
            if (!vectorFile.canRead()) {
                log.error("向量文件保存失败：文件不可读 {}", vectorFile.getAbsolutePath());
                throw new RagException(RagErrorCode.FILE_WRITE_ERROR, "向量文件不可读");
            }
            log.debug("向量文件保存成功，大小：{} bytes", vectorFile.length());
            return vectorFile.getAbsolutePath();
        } catch (Exception e) {
            log.error("保存向量文件失败", e);
            throw new RagException(RagErrorCode.FILE_WRITE_ERROR, e);
        }
    }

    @Override
    public double getProgress() {
        throw new RagException(RagErrorCode.SYSTEM_ERROR, "该方法已废弃，请使用任务管理接口获取进度");
    }

    @Override
    public File getVectorFile() {
        throw new RagException(RagErrorCode.SYSTEM_ERROR, "该方法已废弃，请使用任务管理接口获取文件");
    }
}