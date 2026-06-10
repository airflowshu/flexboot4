package com.yunlbd.flexboot4.dto.oauth;

import java.time.LocalDateTime;

public record UserSocialAccountResp(
        String id,
        String provider,
        String providerUsername,
        String nickname,
        String avatarUrl,
        String emailMasked,
        Boolean emailVerified,
        LocalDateTime bindTime,
        LocalDateTime lastLoginTime
) {
}
