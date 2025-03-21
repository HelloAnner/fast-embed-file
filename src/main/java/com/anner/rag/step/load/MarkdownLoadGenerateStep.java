package com.anner.rag.step.load;


import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Anner
 * @since 12.0
 * Created on 2025/3/20
 */
public class MarkdownLoadGenerateStep implements RagLoadGenerateStep {
    @Override
    public List<Document> loadDocuments(String dir) throws Exception {
        File file = new File(dir);
        if (!file.exists()) {
            throw new Exception("文件不存在");
        }
        if (file.isDirectory()) {
            return Arrays.stream(Objects.requireNonNull(file.listFiles()))
                    .filter(File::isFile)
                    .filter(f -> f.getName().endsWith(".md"))
                    .map(this::loadDocument)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private Document loadDocument(File file) {
        String content;
        try {
            content = FileUtils.readFileToString(file, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Metadata metadata = new Metadata();
        metadata.put("source", file.getAbsolutePath());
        return Document.from(content, metadata);
    }
}
