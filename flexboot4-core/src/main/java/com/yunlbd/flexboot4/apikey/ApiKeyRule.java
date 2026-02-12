package com.yunlbd.flexboot4.apikey;

import java.util.List;

public record ApiKeyRule(
        String apiKeyId,
        String keyHash,
        String userId,
        String tenantId,
        int status,
        List<String> scopes,
        List<String> models,
        Long dailyQuota,
        Long monthlyQuota,
        List<String> permissions
) {
}

