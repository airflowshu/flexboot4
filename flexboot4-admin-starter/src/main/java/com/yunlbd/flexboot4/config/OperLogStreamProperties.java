package com.yunlbd.flexboot4.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "operlog.stream")
public record OperLogStreamProperties(
        String key,
        String group,
        String consumer,
        String deadLetterKey,
        int reclaimBatchSize,
        long reclaimMinIdleMillis,
        int maxDeliveryAttempts
) {
    private static final String DEFAULT_DEAD_LETTER_KEY = "operlog:stream:dead";

    public OperLogStreamProperties {
        if (deadLetterKey == null || deadLetterKey.isBlank()) {
            deadLetterKey = DEFAULT_DEAD_LETTER_KEY;
        }
    }
}

