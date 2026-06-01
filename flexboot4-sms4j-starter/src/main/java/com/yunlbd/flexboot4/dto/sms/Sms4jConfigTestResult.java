package com.yunlbd.flexboot4.dto.sms;

import java.time.LocalDateTime;

public record Sms4jConfigTestResult(
        boolean success,
        String message,
        LocalDateTime testedAt,
        String testStatus
) {
}
