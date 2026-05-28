package com.yunlbd.flexboot4.sms;

import java.util.LinkedHashMap;
import java.util.Map;

public record SmsMessageRequest(
        String phone,
        String templateId,
        Map<String, String> templateParams,
        String configId
) {

    public SmsMessageRequest {
        templateParams = templateParams == null ? Map.of() : new LinkedHashMap<>(templateParams);
    }
}
