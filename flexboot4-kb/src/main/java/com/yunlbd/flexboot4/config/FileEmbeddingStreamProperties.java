package com.yunlbd.flexboot4.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "file.embedding.stream")
public record FileEmbeddingStreamProperties(
        String key,
        String group,
        String consumer,
        int dedupTtlDays
) {
}

