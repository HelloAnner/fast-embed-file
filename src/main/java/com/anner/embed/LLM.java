package com.anner.embed;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.tool.ToolProvider;

import java.util.List;

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
    public static EmbeddingModel doubaoLLMEmbedding(String modelName,String baseUrl, String apiKey) {
        return OpenAiEmbeddingModel.builder()
                .modelName(modelName)
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
    }
}
