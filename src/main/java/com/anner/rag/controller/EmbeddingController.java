package com.anner.rag.controller;

import java.io.File;
import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.anner.rag.model.EmbeddingConfig;
import com.anner.rag.service.EmbeddingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/embedding")
@RequiredArgsConstructor
public class EmbeddingController {
    private final EmbeddingService embeddingService;

    @PostMapping("/process")
    public ResponseEntity<Void> processEmbedding(
            @RequestParam("file") MultipartFile file,
            @RequestParam("modelType") String modelType,
            @RequestParam("baseUrl") String baseUrl,
            @RequestParam("apiKey") String apiKey,
            @RequestParam("maxTokensPerChunk") Integer maxTokensPerChunk,
            @RequestParam("overlapTokens") Integer overlapTokens) {
        EmbeddingConfig config = new EmbeddingConfig();
        config.setFile(file);
        config.setModelType(modelType);
        config.setBaseUrl(baseUrl);
        config.setApiKey(apiKey);
        config.setMaxTokensPerChunk(maxTokensPerChunk);
        config.setOverlapTokens(overlapTokens);

        embeddingService.processEmbedding(config);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/progress")
    public ResponseEntity<Map<String, Double>> getProgress() {
        return ResponseEntity.ok(Map.of("progress", embeddingService.getProgress()));
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadVectorFile() {
        File vectorFile = embeddingService.getVectorFile();
        if (vectorFile == null || !vectorFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(vectorFile);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + vectorFile.getName() + "\"")
                .body(resource);
    }
}