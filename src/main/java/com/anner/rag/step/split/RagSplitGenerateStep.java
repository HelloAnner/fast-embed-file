package com.anner.rag.step.split;

import com.anner.rag.step.RagGenerateStep;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;

import java.util.List;

/**
 * @author Anner
 * @since 12.0
 * Created on 2025/3/20
 */
public interface RagSplitGenerateStep extends RagGenerateStep {
    List<TextSegment> splitIntoChunks(List<Document> documents);
}
