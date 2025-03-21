package com.anner.rag.step.embed;

import com.anner.rag.step.RagGenerateStep;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.List;

/**
 * @author Anner
 * @since 12.0
 * Created on 2025/3/20
 */
public interface RagEmbedGenerateStep extends RagGenerateStep {

    InMemoryEmbeddingStore<TextSegment> generateEmbeddingStore(List<TextSegment> segments);
}
