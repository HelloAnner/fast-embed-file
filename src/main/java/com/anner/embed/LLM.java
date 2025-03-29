package com.anner.embed;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

/**
 * 大语言模型
 *
 * @author Anner
 * @since 11.0
 * Created on 2025/3/5
 */
public class LLM {

    /**
     * Embedding模型
     *
     * @return Embedding模型
     */
    public static EmbeddingModel doubaoLLMEmbedding(String modelName, String baseUrl, String apiKey) {
        return OpenAiEmbeddingModel.builder()
                .modelName(modelName)
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
    }
}
