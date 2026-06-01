package com.yunlbd.flexboot4.media.dto;

import java.util.Map;

public record MediaServerHookInfo(
        String serverId,
        String callbackBaseUrl,
        String adminParams,
        String onStreamChanged,
        String onStreamNoneReader,
        String onServerKeepalive,
        String onRtpServerTimeout,
        Map<String, String> urls
) {
}
