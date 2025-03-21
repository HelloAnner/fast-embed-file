package com.anner.rag.step.embed;


import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.List;

/**
 * @author Anner
 * @since 12.0
 * Created on 2025/3/20
 */
public class LocalEmbedGenerateStep implements RagEmbedGenerateStep {
    @Override
    public InMemoryEmbeddingStore<TextSegment> generateEmbeddingStore(List<TextSegment> segments) {
//        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();
//        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        throw new UnsupportedOperationException("unsupport embed generate step");
    }
}
