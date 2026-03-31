package com.yunlbd.flexboot4.media.dto;

public record PtzControlRequest(
        String channelId,
        String command,
        Integer speed
) {
}
