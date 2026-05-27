package com.yunlbd.flexboot4.lock;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flexboot4.lock")
public record LockProperties(
        String keyPrefix,
        long defaultTtlMillis
) {
}
