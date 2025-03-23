package com.anner.rag.model;

import java.util.List;

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