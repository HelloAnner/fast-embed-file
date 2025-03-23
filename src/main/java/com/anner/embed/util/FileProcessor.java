package com.anner.embed.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModelName;
import dev.langchain4j.model.openai.OpenAiTokenizer;

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
        List<TextSegment> segments = new ArrayList<>();
        String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        
        if (file.getName().endsWith(".json")) {
            segments.addAll(processJsonContent(content, maxTokensPerChunk, overlapTokens));
        } else if (file.getName().endsWith(".md")) {
            segments.addAll(processMarkdownContent(content, maxTokensPerChunk, overlapTokens));
        }
        
        return segments;
    }

    private static List<TextSegment> processJsonContent(String content, int maxTokensPerChunk, int overlapTokens) {
        try {
            JsonNode rootNode = objectMapper.readTree(content);
            List<String> texts = new ArrayList<>();
            extractTexts(rootNode, texts);
            
            DocumentSplitter splitter = DocumentSplitters.recursive(
                maxTokensPerChunk,
                overlapTokens,
                new OpenAiTokenizer(OpenAiEmbeddingModelName.TEXT_EMBEDDING_ADA_002)
            );
            
            List<TextSegment> segments = new ArrayList<>();
            for (String text : texts) {
                segments.addAll(splitter.split(Document.from(text)));
            }
            return segments;
        } catch (IOException e) {
            throw new RuntimeException("解析JSON内容失败", e);
        }
    }

    private static List<TextSegment> processMarkdownContent(String content, int maxTokensPerChunk, int overlapTokens) {
        DocumentSplitter splitter = DocumentSplitters.recursive(
            maxTokensPerChunk,
            overlapTokens,
            new OpenAiTokenizer(OpenAiEmbeddingModelName.TEXT_EMBEDDING_ADA_002)
        );
        return splitter.split(Document.from(content));
    }

    private static void extractTexts(JsonNode node, List<String> texts) {
        if (node.isTextual()) {
            String text = node.asText().trim();
            if (!text.isEmpty()) {
                texts.add(text);
            }
        } else if (node.isObject()) {
            node.fields().forEachRemaining(entry -> extractTexts(entry.getValue(), texts));
        } else if (node.isArray()) {
            node.elements().forEachRemaining(element -> extractTexts(element, texts));
        }
    }
}