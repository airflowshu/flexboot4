package com.yunlbd.flexboot4.oauth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "flexboot4.auth.oauth.github")
public class GithubOAuthProperties {

    private String clientId;
    private String clientSecret;
    private String authorizeUrl = "https://github.com/login/oauth/authorize";
    private String tokenUrl = "https://github.com/login/oauth/access_token";
    private String userUrl = "https://api.github.com/user";
    private String emailsUrl = "https://api.github.com/user/emails";
    private String scope = "read:user user:email";
    private String callbackUri;
}
