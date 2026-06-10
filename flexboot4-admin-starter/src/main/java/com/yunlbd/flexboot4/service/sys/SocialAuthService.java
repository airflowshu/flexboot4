package com.yunlbd.flexboot4.service.sys;

import com.yunlbd.flexboot4.dto.LoginResp;
import com.yunlbd.flexboot4.dto.oauth.OAuthBindReq;
import com.yunlbd.flexboot4.dto.oauth.OAuthCallbackResultResp;
import com.yunlbd.flexboot4.dto.oauth.UserSocialAccountResp;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface SocialAuthService {

    String buildAuthorizeUrl(String provider, HttpServletRequest request);

    String handleCallback(String provider, String code, String state, HttpServletRequest request);

    OAuthCallbackResultResp consumeResult(String ticket);

    LoginResp bind(String provider, OAuthBindReq req, String clientIp);

    List<UserSocialAccountResp> listBoundAccounts(String userId);

    void unbind(String userId, String accountId);
}
