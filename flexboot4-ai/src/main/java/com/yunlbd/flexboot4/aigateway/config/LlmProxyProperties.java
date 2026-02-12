package com.yunlbd.flexboot4.aigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "llm-proxy")
public record LlmProxyProperties(String url, String chatPath, Duration timeout) {
}
