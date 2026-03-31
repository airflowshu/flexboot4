package com.yunlbd.flexboot4.media.dto;

import java.time.LocalDateTime;

public record PlaybackStartRequest(
        String channelId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String protocol
) {
}
