package com.yunlbd.flexboot4.aigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai-routing")
public record AiRoutingProperties(String primaryModel, String fallbackModel) {
}
