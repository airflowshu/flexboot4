package com.yunlbd.flexboot4.oauth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "flexboot4.auth.oauth.qq")
public class QqOAuthProperties {

    private String appId;
    private String appKey;
    private String authorizeUrl = "https://graph.qq.com/oauth2.0/authorize";
    private String tokenUrl = "https://graph.qq.com/oauth2.0/token";
    private String openidUrl = "https://graph.qq.com/oauth2.0/me";
    private String userInfoUrl = "https://graph.qq.com/user/get_user_info";
    private String scope = "get_user_info";
    private String callbackUri;
}
