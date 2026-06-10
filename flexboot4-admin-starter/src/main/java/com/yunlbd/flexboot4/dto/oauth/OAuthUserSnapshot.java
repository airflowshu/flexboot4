package com.yunlbd.flexboot4.dto.oauth;

public record OAuthUserSnapshot(
        String provider,
        String providerUsername,
        String nickname,
        String avatarUrl,
        String email,
        Boolean emailVerified
) {
}
