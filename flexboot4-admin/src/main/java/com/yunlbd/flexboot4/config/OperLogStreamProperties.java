package com.yunlbd.flexboot4.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "operlog.stream")
public record OperLogStreamProperties(
        String key,
        String group,
        String consumer,
        int dedupTtlDays
) {
}

