package com.anner.rag.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;

public class FileProcessor {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static List<TextSegment> processCompressedFile(String filePath, int maxTokensPerChunk, int overlapTokens)
            throws Exception {
        List<TextSegment> segments = new ArrayList<>();

        try (InputStream is = new FileInputStream(filePath);
                ArchiveInputStream archiveInputStream = new ArchiveStreamFactory()
                        .createArchiveInputStream(new BufferedInputStream(is))) {

            ArchiveEntry entry;
            while ((entry = archiveInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String extension = FilenameUtils.getExtension(entry.getName()).toLowerCase();
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    IOUtils.copy(archiveInputStream, outputStream);
                    String content = outputStream.toString(StandardCharsets.UTF_8.name());

                    // 根据文件类型处理内容
                    Document document;
                    if (extension.equals("json")) {
                        document = processJsonContent(content, entry.getName());
                    } else if (extension.equals("md")) {
                        document = processMarkdownContent(content, entry.getName());
                    } else {
                        continue;
                    }

                    // 处理文件内容
                    List<TextSegment> fileSegments = segmentText(document, maxTokensPerChunk, overlapTokens);
                    segments.addAll(fileSegments);
                }
            }
        }

        return segments;
    }

    private static Document processJsonContent(String content, String fileName) throws IOException {
        JsonNode jsonNode = objectMapper.readTree(content);
        Metadata metadata = new Metadata();
        StringBuilder textContent = new StringBuilder();

        // 添加基础元数据
        metadata.put("source_type", "json");
        metadata.put("file_name", fileName);

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
        DocumentSplitter splitter = DocumentSplitters.recursive(
                maxTokensPerChunk, // 最大分块大小
                overlapTokens, // 重叠大小
                null // 使用默认的分词器
        );

        // 分割文档并返回文本段
        return splitter.split(document);
    }
}