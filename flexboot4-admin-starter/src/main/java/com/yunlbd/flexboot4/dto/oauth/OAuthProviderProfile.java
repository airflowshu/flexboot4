package com.yunlbd.flexboot4.dto.oauth;

public record OAuthProviderProfile(
        String provider,
        String providerUserId,
        String providerUsername,
        String nickname,
        String avatarUrl,
        String email,
        boolean emailVerified
) {
}
