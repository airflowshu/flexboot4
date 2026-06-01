package com.yunlbd.flexboot4.dto.sms;

import java.util.LinkedHashMap;
import java.util.Map;

public record Sms4jConfigTestReq(
        String phone,
        String templateId,
        Map<String, String> templateParams
) {

    public Sms4jConfigTestReq {
        templateParams = templateParams == null ? new LinkedHashMap<>() : new LinkedHashMap<>(templateParams);
    }
}
