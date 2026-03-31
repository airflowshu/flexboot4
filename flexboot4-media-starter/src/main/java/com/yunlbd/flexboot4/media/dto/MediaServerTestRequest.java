package com.yunlbd.flexboot4.media.dto;

public record MediaServerTestRequest(
        String serverId,
        String baseUrl,
        String apiSecret
) {
}
