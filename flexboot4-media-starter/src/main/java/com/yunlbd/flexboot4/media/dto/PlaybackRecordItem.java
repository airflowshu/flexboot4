package com.yunlbd.flexboot4.media.dto;

import java.time.LocalDateTime;

public record PlaybackRecordItem(
        String deviceId,
        String name,
        String address,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String secrecy
) {
}
