package com.anner.embed.model;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class EmbeddingConfig {
    private MultipartFile file;
    private String modelType;
    private String baseUrl;
    private String apiKey;
    private Integer maxTokensPerChunk;
    private Integer overlapTokens;
}