package com.yunlbd.flexboot4.dto.oauth;

import com.yunlbd.flexboot4.dto.LoginResp;

import java.util.List;

public record OAuthCallbackResultResp(
        OAuthCallbackStatus status,
        LoginResp login,
        String bindTicket,
        OAuthUserSnapshot externalUser,
        List<OAuthBindCandidate> candidates,
        String message
) {
}
