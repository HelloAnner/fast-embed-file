package com.anner.rag.step.embed;


import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.List;

/**
 * @author Anner
 * @since 12.0
 * Created on 2025/3/20
 */
public class OllamaEmbedGenerateStep implements RagEmbedGenerateStep {
    @Override
    public InMemoryEmbeddingStore<TextSegment> generateEmbeddingStore(List<TextSegment> segments) {
        // EmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
        // .baseUrl("http://localhost:11434")
        // .modelName("nomic-embed-text")
        // .build();

//        List<Embedding> embeddings = new ArrayList<>();

        // 将segments进行分批处理
        // for (int i = 0; i < segments.size(); i += AssistantConstants.BATCH_SIZE) {
        // int end = Math.min(i + AssistantConstants.BATCH_SIZE, segments.size());
        // embeddings.addAll(embeddingModel.embedAll(segments.subList(i,
        // end)).content());
        // log.info("embedding , {}/{}", i, segments.size());
        // }

        throw new UnsupportedOperationException("unsupported ollama embedding store");
    }
}
