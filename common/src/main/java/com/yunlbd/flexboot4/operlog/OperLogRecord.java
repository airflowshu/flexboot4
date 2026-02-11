package com.yunlbd.flexboot4.operlog;

import java.util.Map;

public record OperLogRecord(
        String eventId,
        String title,
        int businessType,
        int operatorType,
        String method,
        String requestMethod,
        String operUrl,
        String operIp,
        Map<String, String> terminal,
        String operName,
        String operUserId,
        String deptId,
        long operTimeEpochMillis,
        long costTimeMillis,
        int status,
        String errorMsg,
        Map<String, Object> operParam,
        Map<String, Object> jsonResult,
        Map<String, Object> extParams
) {
}

