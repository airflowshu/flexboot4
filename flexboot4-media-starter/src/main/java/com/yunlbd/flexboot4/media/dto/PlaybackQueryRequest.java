package com.yunlbd.flexboot4.media.dto;

import java.time.LocalDateTime;

public record PlaybackQueryRequest(
        String channelId,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
