package com.yunlbd.flexboot4.media.dto;

public record GatewayReloadRequest(
        String gatewayId,
        Boolean autoStart
) {
}
