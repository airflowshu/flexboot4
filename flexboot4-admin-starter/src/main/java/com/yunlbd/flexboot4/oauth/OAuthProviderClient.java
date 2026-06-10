package com.yunlbd.flexboot4.oauth;

import com.yunlbd.flexboot4.dto.oauth.OAuthProviderProfile;

public interface OAuthProviderClient {

    String provider();

    boolean configured();

    default String callbackUri(String fallback) {
        return fallback;
    }

    String buildAuthorizeUrl(String state, String redirectUri);

    OAuthProviderProfile fetchProfile(String code, String redirectUri);
}
