package com.yunlbd.flexboot4.dto.oauth;

public record OAuthBindCandidate(
        String userId,
        String username,
        String realName,
        String emailMasked
) {
}
