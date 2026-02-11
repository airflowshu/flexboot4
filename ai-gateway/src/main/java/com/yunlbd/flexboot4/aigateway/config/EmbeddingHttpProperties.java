package com.yunlbd.flexboot4.aigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "embedding-http")
public record EmbeddingHttpProperties(String url, Duration timeout) {
}
