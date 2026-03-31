package com.yunlbd.flexboot4.media.dto;

import java.util.Map;

public record MediaPlayResponse(
        String sessionId,
        String status,
        String app,
        String stream,
        String protocol,
        Map<String, String> urls
) {
}
