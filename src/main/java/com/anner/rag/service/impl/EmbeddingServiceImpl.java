package com.anner.rag.service.impl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import com.anner.LLM;
import com.anner.rag.model.EmbeddingConfig;
import com.anner.rag.service.EmbeddingService;
import com.anner.rag.step.embed.DoubaoEmbedGenerateStep;
import com.anner.rag.util.FileProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EmbeddingServiceImpl() {
        // 确保向量文件目录存在
        new File(VECTOR_DIR).mkdirs();
    }

    @Override
    public CompletableFuture<Void> processEmbedding(EmbeddingConfig config) {
        return CompletableFuture.runAsync(() -> {
            try {
                // 重置进度和当前文件
                totalSegments.set(0);
                processedSegments.set(0);
                currentVectorFile = null;

                // 保存上传的文件
                Path tempDir = Files.createTempDirectory("embedding_");
                File uploadedFile = new File(tempDir.toFile(), config.getFile().getOriginalFilename());
                config.getFile().transferTo(uploadedFile);

                // 解压并处理文件
                List<TextSegment> segments = FileProcessor.processCompressedFile(
                        uploadedFile.getAbsolutePath(),
                        config.getMaxTokensPerChunk(),
                        config.getOverlapTokens());

                totalSegments.set(segments.size());

                // 创建自定义的DoubaoEmbedGenerateStep，使用进度跟踪
                DoubaoEmbedGenerateStep embedStep = new DoubaoEmbedGenerateStep() {
                    @Override
                    public InMemoryEmbeddingStore<TextSegment> generateEmbeddingStore(List<TextSegment> segments) {
                        EmbeddingModel embeddingModel = LLM.doubaoLLMEmbedding(config.getModelType(),
                                config.getApiKey());
                        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

                        for (TextSegment segment : segments) {
                            Embedding embedding = embeddingModel.embed(segment.text()).content();
                            embeddingStore.add(embedding, segment);
                            processedSegments.incrementAndGet();
                            log.info("向量化处理进度：{}%", getProgress());
                        }

                        return embeddingStore;
                    }
                };

                // 执行向量化
                InMemoryEmbeddingStore<TextSegment> embeddingStore = embedStep.generateEmbeddingStore(segments);

                // 保存向量化的结果
                String fileName = UUID.randomUUID().toString() + ".json";
                File vectorFile = new File(VECTOR_DIR, fileName);
                objectMapper.writeValue(vectorFile, embeddingStore);
                currentVectorFile = fileName;

                // 清理临时文件
                Files.deleteIfExists(uploadedFile.toPath());
                Files.deleteIfExists(tempDir);

                log.info("向量化处理完成，共处理 {} 个文本段，保存到文件：{}", segments.size(), vectorFile.getAbsolutePath());
            } catch (Exception e) {
                log.error("向量化处理失败", e);
                throw new RuntimeException("向量化处理失败", e);
            }
        });
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
            return null;
        }
        return new File(VECTOR_DIR, currentVectorFile);
    }
}