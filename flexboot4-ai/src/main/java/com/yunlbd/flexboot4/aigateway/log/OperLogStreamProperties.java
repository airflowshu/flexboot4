package com.yunlbd.flexboot4.aigateway.log;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "operlog.stream")
public record OperLogStreamProperties(String key) {
}

