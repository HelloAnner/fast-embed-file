package com.anner.rag.service;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import com.anner.rag.model.EmbeddingConfig;

public interface EmbeddingService {
    CompletableFuture<Void> processEmbedding(EmbeddingConfig config);

    double getProgress();

    File getVectorFile();
}