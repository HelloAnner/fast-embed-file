package com.anner;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

/**
 * 大语言模型
 *
 * @author Anner
 * @since 11.0
 *        Created on 2025/3/5
 */
public class LLM {

    /**
     * Embedding模型
     *
     * @return Embedding模型
     */
    public static EmbeddingModel doubaoLLMEmbedding(String baseUrl, String apiKey) {
        return OpenAiEmbeddingModel.builder()
                .modelName("doubao-embedding-text-240715")
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
    }
}
