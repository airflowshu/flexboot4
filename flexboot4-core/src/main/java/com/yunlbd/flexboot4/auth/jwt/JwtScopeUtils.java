package com.yunlbd.flexboot4.auth.jwt;

import java.util.Collection;

public final class JwtScopeUtils {
    private JwtScopeUtils() {
    }

    public static boolean hasScope(Collection<String> scopes, String requiredScope) {
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

