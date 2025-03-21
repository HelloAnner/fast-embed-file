package com.anner.rag.model;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class EmbeddingConfig {
    private String modelType;
    private String baseUrl;
    private String apiKey;
    private MultipartFile file;
    private Integer maxTokensPerChunk;
    private Integer overlapTokens;
}