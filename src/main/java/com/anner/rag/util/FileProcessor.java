package com.anner.rag.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.anner.rag.RagConstants;
import dev.langchain4j.model.openai.OpenAiEmbeddingModelName;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;

public class FileProcessor {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DEFAULT_MODEL_TYPE = "doubao-embedding-text-240715";

    public static List<TextSegment> processFiles(String directoryPath, int maxTokensPerChunk, int overlapTokens)
            throws Exception {
        List<TextSegment> segments = new ArrayList<>();
        File directory = new File(directoryPath);

        if (!directory.exists() || !directory.isDirectory()) {
            throw new IllegalArgumentException("提供的路径不是一个有效的目录：" + directoryPath);
        }

        // 递归处理目录中的所有文件
        processDirectory(directory, segments, maxTokensPerChunk, overlapTokens);

        return segments;
    }

    private static void processDirectory(File directory, List<TextSegment> segments, int maxTokensPerChunk,
                                         int overlapTokens)
            throws Exception {
        File[] files = directory.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            if (file.isDirectory()) {
                processDirectory(file, segments, maxTokensPerChunk, overlapTokens);
            } else {
                String extension = FilenameUtils.getExtension(file.getName()).toLowerCase();
                if (extension.equals("json") || extension.equals("md")) {
                    Document document;
                    String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

                    if (extension.equals("json")) {
                        document = processJsonContent(content, file.getName());
                    } else {
                        document = processMarkdownContent(content, file.getName());
                    }

                    // 处理文件内容
                    List<TextSegment> fileSegments = segmentText(document, maxTokensPerChunk, overlapTokens);
                    segments.addAll(fileSegments);
                }
            }
        }
    }

    private static Document processJsonContent(String content, String fileName) throws IOException {
        JsonNode jsonNode = objectMapper.readTree(content);
        Metadata metadata = new Metadata();
        StringBuilder textContent = new StringBuilder();

        // 添加基础元数据
        metadata.put("source_type", "json");
        metadata.put("file_name", fileName);
        metadata.put("model_type", DEFAULT_MODEL_TYPE);

        // 处理JSON字段
        for (Iterator<Map.Entry<String, JsonNode>> it = jsonNode.fields(); it.hasNext();) {
            Map.Entry<String, JsonNode> entry = it.next();
            String key = entry.getKey();
            JsonNode value = entry.getValue();

            // 将字段名和值都添加到元数据中
            metadata.put(key, value.asText());

            // 将内容添加到文本中
            textContent.append(key).append(": ").append(value.asText()).append("\n");
        }

        return Document.from(textContent.toString(), metadata);
    }

    private static Document processMarkdownContent(String content, String fileName) {
        Metadata metadata = new Metadata();

        // 添加基础元数据
        metadata.put("source_type", "markdown");
        metadata.put("file_name", fileName);
        metadata.put("model_type", DEFAULT_MODEL_TYPE);

        // 提取标题（如果有）
        String[] lines = content.split("\n");
        if (lines.length > 0 && lines[0].startsWith("# ")) {
            metadata.put("title", lines[0].substring(2).trim());
        }

        // 计算一些基本统计信息
        metadata.put("word_count", String.valueOf(content.split("\\s+").length));
        metadata.put("char_count", String.valueOf(content.length()));

        return Document.from(content, metadata);
    }

    private static List<TextSegment> segmentText(Document document, int maxTokensPerChunk, int overlapTokens) {
        // 创建文档分割器
        OpenAiTokenizer tokenizer = new OpenAiTokenizer(OpenAiEmbeddingModelName.TEXT_EMBEDDING_ADA_002);

        DocumentSplitter splitter = DocumentSplitters.recursive(
               maxTokensPerChunk,
              overlapTokens,
                tokenizer);

        // 分割文档并返回文本段
        return splitter.split(document);
    }
}