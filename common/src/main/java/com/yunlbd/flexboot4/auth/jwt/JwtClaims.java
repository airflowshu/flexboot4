package com.yunlbd.flexboot4.auth.jwt;

import java.util.List;

public record JwtClaims(
        String sub,
        String username,
        List<String> roles,
        List<String> scope,
        long iat,
        long exp
) {
}

