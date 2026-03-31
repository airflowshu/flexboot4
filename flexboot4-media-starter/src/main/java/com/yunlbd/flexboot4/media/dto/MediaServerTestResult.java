package com.yunlbd.flexboot4.media.dto;

import java.time.LocalDateTime;

public record MediaServerTestResult(
        boolean success,
        String version,
        int streamCount,
        LocalDateTime testedAt,
        String message
) {
}
