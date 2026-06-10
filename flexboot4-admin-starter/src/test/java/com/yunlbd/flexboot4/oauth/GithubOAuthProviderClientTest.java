package com.yunlbd.flexboot4.oauth;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class GithubOAuthProviderClientTest {

    @Test
    void buildAuthorizeUrlEncodesQueryParams() {
        GithubOAuthProperties properties = new GithubOAuthProperties();
        properties.setClientId("client id");
        properties.setClientSecret("client secret");
        properties.setAuthorizeUrl("https://github.com/login/oauth/authorize");
        properties.setScope("read:user user:email");
        GithubOAuthProviderClient client = new GithubOAuthProviderClient(
                properties,
                RestClient.builder()
        );

        String url = client.buildAuthorizeUrl(
                "state value",
                "http://localhost:8080/api/admin/auth/oauth/github/callback"
        );

        assertThat(url).contains("client_id=client%20id");
        assertThat(url).contains("scope=read:user%20user:email");
        assertThat(url).contains("state=state%20value");
        assertThat(URI.create(url).getHost()).isEqualTo("github.com");
    }

    @Test
    void callbackUriUsesConfiguredValueWhenPresent() {
        GithubOAuthProperties properties = new GithubOAuthProperties();
        properties.setCallbackUri("http://localhost:8080/api/admin/auth/oauth/github/callback");
        GithubOAuthProviderClient client = new GithubOAuthProviderClient(
                properties,
                RestClient.builder()
        );

        assertThat(client.callbackUri("http://192.168.6.7:8080/api/admin/auth/oauth/github/callback"))
                .isEqualTo("http://localhost:8080/api/admin/auth/oauth/github/callback");
    }
}
