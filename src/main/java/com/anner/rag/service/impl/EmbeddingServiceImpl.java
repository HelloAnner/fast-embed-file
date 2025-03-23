package com.anner.rag.service.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.anner.LLM;
import com.anner.rag.exception.RagErrorCode;
import com.anner.rag.exception.RagException;
import com.anner.rag.model.EmbeddingConfig;
import com.anner.rag.service.EmbeddingService;
import com.anner.rag.util.FileProcessor;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmbeddingServiceImpl implements EmbeddingService {
    private final AtomicInteger totalSegments = new AtomicInteger(0);
    private final AtomicInteger processedSegments = new AtomicInteger(0);
    private String currentVectorFile;
    private static final String VECTOR_DIR = "vectors";
    private static final String UPLOAD_DIR = "upload_files";

    public EmbeddingServiceImpl() {
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
    public CompletableFuture<Void> processEmbedding(EmbeddingConfig config) {
        try {
            validateConfig(config);

            // 重置进度和当前文件
            totalSegments.set(0);
            processedSegments.set(0);
            currentVectorFile = null;

            // 生成唯一的工作目录
            String workDirName = UUID.randomUUID().toString();
            File workDir = new File(UPLOAD_DIR, workDirName);
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
                    processExtractedFiles(workDirName,config, extractDirPath);
                } catch (Exception e) {
                    log.error("向量化处理失败", e);
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

    private void processExtractedFiles(String uuid,EmbeddingConfig config, String extractDirPath) {
        try {
            // 处理解压后的文件
            List<TextSegment> segments = FileProcessor.processFiles(
                    extractDirPath,
                    config.getMaxTokensPerChunk(),
                    config.getOverlapTokens());

            totalSegments.set(segments.size());

            // 执行向量化
            InMemoryEmbeddingStore<TextSegment> embeddingStore = generateEmbeddingStore(config, segments);

            // 保存向量化的结果
            saveVectorFile(uuid,embeddingStore, segments.size());
        } catch (Exception e) {
            throw new RagException(RagErrorCode.VECTORIZATION_FAILED, e);
        }
    }

    private InMemoryEmbeddingStore<TextSegment> generateEmbeddingStore(EmbeddingConfig config,
            List<TextSegment> segments) {
        try {
            EmbeddingModel embeddingModel = LLM.doubaoLLMEmbedding(config.getModelType(), config.getBaseUrl(),
                    config.getApiKey());
            InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

            for (TextSegment segment : segments) {
                try {
                    Embedding embedding = embeddingModel.embed(segment.text()).content();
                    embeddingStore.add(embedding, segment);
                    processedSegments.incrementAndGet();
                    log.info("向量化处理进度：{}%", getProgress());
                } catch (Exception e) {
                    throw new RagException(RagErrorCode.MODEL_ERROR, "处理文本段失败: " + segment.text());
                }
            }

            return embeddingStore;
        } catch (Exception e) {
            throw new RagException(RagErrorCode.API_ERROR, e);
        }
    }

    private void saveVectorFile(String uuid,InMemoryEmbeddingStore<TextSegment> embeddingStore, int segmentsSize) {
        try {
            String fileName = uuid + ".json";
            File vectorFile = new File(VECTOR_DIR, fileName);
            embeddingStore.serializeToFile(vectorFile.getAbsolutePath());
            currentVectorFile = fileName;
            log.info("向量化处理完成，共处理 {} 个文本段，保存到文件：{}", segmentsSize, vectorFile.getAbsolutePath());
        } catch (Exception e) {
            throw new RagException(RagErrorCode.FILE_WRITE_ERROR, e);
        }
    }

    @Override
    public double getProgress() {
        int total = totalSegments.get();
        if (total == 0)
            return 0.0;
        return (double) processedSegments.get() / total * 100;
    }

    @Override
    public File getVectorFile() {
        if (currentVectorFile == null) {
            throw new RagException(RagErrorCode.FILE_NOT_FOUND, "向量化文件不存在");
        }
        File file = new File(VECTOR_DIR, currentVectorFile);
        if (!file.exists()) {
            throw new RagException(RagErrorCode.FILE_NOT_FOUND, "向量化文件不存在");
        }
        return file;
    }
}