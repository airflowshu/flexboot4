package com.yunlbd.flexboot4.auth.context;

import java.util.List;

public record AuthContext(
        AuthType authType,
        String userId,
        String tenantId,
        String username,
        List<String> scopes,
        List<String> roles,
        List<String> permissions,
        String apiKeyId,
        String apiKeyHash,
        List<String> allowedModels,
        Long dailyQuota,
        Long monthlyQuota
) {
    public boolean hasScope(String requiredScope) {
        if (requiredScope == null || requiredScope.isBlank()) {
            return true;
        }
        if (scopes == null || scopes.isEmpty()) {
            return false;
        }
        for (String s : scopes) {
            if (requiredScope.equals(s)) {
                return true;
            }
        }
        return false;
    }
}

