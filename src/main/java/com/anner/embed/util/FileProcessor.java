package com.anner.embed.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModelName;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FileProcessor {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static List<TextSegment> processFiles(List<File> files, int maxTokensPerChunk, int overlapTokens) {
        List<TextSegment> allSegments = new ArrayList<>();
        for (File file : files) {
            try {
                allSegments.addAll(processFile(file, maxTokensPerChunk, overlapTokens));
            } catch (IOException e) {
                throw new RuntimeException("处理文件失败: " + file.getName(), e);
            }
        }
        return allSegments;
    }

    public static List<TextSegment> processFile(File file, int maxTokensPerChunk, int overlapTokens) throws IOException {
        String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        return new ArrayList<>(processContent(content, maxTokensPerChunk, overlapTokens));
    }

    private static List<TextSegment> processContent(String content, int maxTokensPerChunk, int overlapTokens) {
        DocumentSplitter splitter = DocumentSplitters.recursive(
                maxTokensPerChunk,
                overlapTokens,
                new OpenAiTokenizer(OpenAiEmbeddingModelName.TEXT_EMBEDDING_ADA_002)
        );
        return splitter.split(Document.from(content));
    }
}