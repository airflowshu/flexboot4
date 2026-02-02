package com.yunlbd.flexboot4.aigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag")
public record RagProperties(String defaultEmbeddingModel, Integer defaultTopK, Integer maxContextChars, String defaultSystemPrompt) {
}
