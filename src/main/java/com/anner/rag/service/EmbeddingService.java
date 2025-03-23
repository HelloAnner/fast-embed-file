package com.anner.rag.service;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import com.anner.rag.model.EmbeddingConfig;
import org.apache.commons.compress.archivers.ArchiveException;

public interface EmbeddingService {
    CompletableFuture<Void> processEmbedding(String taskId,EmbeddingConfig config) throws IOException, ArchiveException;

    double getProgress();

    File getVectorFile();
}