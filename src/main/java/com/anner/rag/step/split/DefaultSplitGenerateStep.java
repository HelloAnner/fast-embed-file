package com.anner.rag.step.split;


import com.anner.rag.RagConstants;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModelName;
import dev.langchain4j.model.openai.OpenAiTokenizer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Anner
 * @since 12.0
 * Created on 2025/3/20
 */
public class DefaultSplitGenerateStep implements RagSplitGenerateStep {
    @Override
    public List<TextSegment> splitIntoChunks(List<Document> documents) {
        OpenAiTokenizer tokenizer = new OpenAiTokenizer(OpenAiEmbeddingModelName.TEXT_EMBEDDING_ADA_002);

        DocumentSplitter splitter = DocumentSplitters.recursive(
                RagConstants.SPLIT_PARAMS.MAX_TOKENS_PER_CHUNK,
                RagConstants.SPLIT_PARAMS.OVER_LAP_TOKENS,
                tokenizer);

        List<TextSegment> allSegments = new ArrayList<>();
        for (Document document : documents) {
            List<TextSegment> segments = splitter.split(document);
            allSegments.addAll(segments);
        }

        return allSegments;
    }
}
