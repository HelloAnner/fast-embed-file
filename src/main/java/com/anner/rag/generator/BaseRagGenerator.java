package com.anner.rag.generator;

import com.anner.rag.step.embed.DoubaoEmbedGenerateStep;
import com.anner.rag.step.embed.RagEmbedGenerateStep;
import com.anner.rag.step.load.RagLoadGenerateStep;
import com.anner.rag.step.split.DefaultSplitGenerateStep;
import com.anner.rag.step.split.RagSplitGenerateStep;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Anner
 * @since 12.0
 * Created on 2025/3/20
 */
public abstract class BaseRagGenerator implements RagGenerator {
    protected final String sourceDir;

    protected final String targetFile;

    @Override
    public String sourceDirPath() {
        return sourceDir;
    }

    @Override
    public String outputFilePath() {
        return targetFile;
    }

    public BaseRagGenerator(String sourceDir, String targetFile) {
        this.sourceDir = sourceDir;
        this.targetFile = targetFile;
    }

    @Override
    public RagSplitGenerateStep splitStep() {
        // 默认参数划分
        return new DefaultSplitGenerateStep();
    }

    @Override
    public RagEmbedGenerateStep embedStep() {
        // 默认使用doubao的embed 模型
        return new DoubaoEmbedGenerateStep();
    }

    @Override
    public void generate() throws Exception {
        List<Document> documents = new ArrayList<>();
        for (RagLoadGenerateStep loadGenerateStep : loadStep()) {
            documents.addAll(loadGenerateStep.loadDocuments(sourceDirPath()));
        }
        List<TextSegment> allSegments = splitStep().splitIntoChunks(documents);
        InMemoryEmbeddingStore<TextSegment> embeddingStore = embedStep().generateEmbeddingStore(allSegments);
        // 存储到文件
        embeddingStore.serializeToFile(outputFilePath());
    }
}
