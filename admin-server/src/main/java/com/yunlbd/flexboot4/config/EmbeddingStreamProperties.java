package com.yunlbd.flexboot4.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "embedding.stream")
public record EmbeddingStreamProperties(String key, String dlqKey) {
}
