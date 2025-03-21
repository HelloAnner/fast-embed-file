package com.anner.rag.step.embed;


import java.util.List;

import com.anner.LLM;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

/**
 * @author Anner
 * @since 12.0
 * Created on 2025/3/20
 */
public class DoubaoEmbedGenerateStep implements RagEmbedGenerateStep {
    @Override
    public InMemoryEmbeddingStore<TextSegment> generateEmbeddingStore(List<TextSegment> segments) {
        EmbeddingModel embeddingModel = LLM.doubaoLLMEmbedding("", "");
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            Embedding embedding = embeddingModel.embed(segment.text()).content();
            embeddingStore.add(embedding, segment);
        }

        return embeddingStore;
    }
}
