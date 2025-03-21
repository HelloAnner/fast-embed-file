package com.anner.rag.step.load;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 加载 json 格式的源文件
 *
 * @author Anner
 * @since 12.0
 * Created on 2025/3/20
 */
public class JsonLoadGenerateStep implements RagLoadGenerateStep {

    @Override
    public List<Document> loadDocuments(String dir) throws Exception {
        File docsDir = new File(dir);
        File[] jsonFiles = docsDir.listFiles((d, name) -> name.endsWith(".json"));
        List<Document> batchDocuments = new ArrayList<>();
        for (File jsonFile : jsonFiles) {
            batchDocuments.add(loadOneFile(jsonFile.getAbsolutePath()));
        }

        return batchDocuments;
    }

    private Document loadOneFile(String resourcePath) throws IOException {
        InputStream inputStream = Files.newInputStream(Paths.get(resourcePath));

        ObjectMapper objectMapper = new ObjectMapper();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        StringBuilder text = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            text.append(line);
        }

        JsonNode jsonNode = objectMapper.readTree(text.toString());
        Metadata metadata = new Metadata();
        StringBuilder content = new StringBuilder();
        for (Iterator<Map.Entry<String, JsonNode>> it = jsonNode.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            metadata.put(entry.getKey(), entry.getValue().asText());
            content.append(entry.getValue().asText()).append("\n");
        }
        return Document.from(content.toString(), metadata);
    }
}
