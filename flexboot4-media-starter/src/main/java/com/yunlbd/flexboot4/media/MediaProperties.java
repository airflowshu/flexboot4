package com.yunlbd.flexboot4.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "media")
public record MediaProperties(
        boolean enabled,
        String callbackBaseUrl,
        String defaultPlayProtocol,
        String snapshotBizType,
        int gatewayCoreThreads,
        int gatewayMaxThreads,
        int gatewayQueueCapacity,
        String hookSecretHeader,
        long hookTimestampToleranceSeconds,
        boolean runtimeCheckEnabled,
        long runtimeCheckInitialDelayMillis,
        long runtimeCheckFixedDelayMillis,
        long serverHookTimeoutSeconds,
        long deviceKeepaliveTimeoutSeconds,
        long pendingSessionTimeoutSeconds,
        long streamingSessionTimeoutSeconds,
        boolean gatewayAutoRecover
) {
}
