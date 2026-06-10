package com.yunlbd.flexboot4.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunlbd.flexboot4.dto.oauth.OAuthProviderProfile;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class QqOAuthProviderClientTest {

    @Test
    void buildAuthorizeUrlEncodesQueryParams() {
        QqOAuthProviderClient client = new QqOAuthProviderClient(
                configuredProperties(),
                RestClient.builder(),
                new ObjectMapper()
        );

        String url = client.buildAuthorizeUrl(
                "state value",
                "http://localhost:8080/api/admin/auth/oauth/qq/callback"
        );

        assertThat(url).contains("response_type=code");
        assertThat(url).contains("client_id=appid%201");
        assertThat(url).contains("scope=get_user_info");
        assertThat(url).contains("state=state%20value");
        assertThat(URI.create(url).getHost()).isEqualTo("graph.qq.com");
    }

    @Test
    void configuredRequiresAppIdAndAppKey() {
        QqOAuthProperties properties = new QqOAuthProperties();
        QqOAuthProviderClient client = new QqOAuthProviderClient(
                properties,
                RestClient.builder(),
                new ObjectMapper()
        );

        assertThat(client.configured()).isFalse();

        properties.setAppId("appid");
        properties.setAppKey("appkey");

        assertThat(client.configured()).isTrue();
    }

    @Test
    void callbackUriUsesConfiguredValueWhenPresent() {
        QqOAuthProperties properties = configuredProperties();
        properties.setCallbackUri("http://localhost:8080/api/admin/auth/oauth/qq/callback");
        QqOAuthProviderClient client = new QqOAuthProviderClient(
                properties,
                RestClient.builder(),
                new ObjectMapper()
        );

        assertThat(client.callbackUri("http://192.168.6.7:8080/api/admin/auth/oauth/qq/callback"))
                .isEqualTo("http://localhost:8080/api/admin/auth/oauth/qq/callback");
    }

    @Test
    void parseAccessTokenSupportsFormAndJsonResponses() {
        QqOAuthProviderClient client = new QqOAuthProviderClient(
                configuredProperties(),
                RestClient.builder(),
                new ObjectMapper()
        );

        assertThat(client.parseAccessToken("access_token=token%201&expires_in=7776000&refresh_token=refresh"))
                .isEqualTo("token 1");
        assertThat(client.parseAccessToken("{\"access_token\":\"json-token\",\"expires_in\":7776000}"))
                .isEqualTo("json-token");
    }

    @Test
    void parseOpenidSupportsJsonAndCallbackResponses() {
        QqOAuthProviderClient client = new QqOAuthProviderClient(
                configuredProperties(),
                RestClient.builder(),
                new ObjectMapper()
        );

        assertThat(client.parseOpenid("{\"client_id\":\"appid\",\"openid\":\"openid-1\"}"))
                .isEqualTo("openid-1");
        assertThat(client.parseOpenid("callback( {\"client_id\":\"appid\",\"openid\":\"openid-2\"} );"))
                .isEqualTo("openid-2");
    }

    @Test
    void fetchProfileMapsQqUserInfo() {
        AtomicInteger index = new AtomicInteger();
        String[] bodies = {
                "access_token=token-1&expires_in=7776000",
                "callback( {\"client_id\":\"appid\",\"openid\":\"openid-1\"} );",
                "{\"ret\":0,\"nickname\":\"Alice QQ\",\"figureurl_qq_2\":\"https://q.qlogo.cn/2.png\",\"figureurl_qq_1\":\"https://q.qlogo.cn/1.png\"}"
        };
        RestClient.Builder builder = RestClient.builder()
                .requestInterceptor((request, body, execution) -> new ClientHttpResponse() {
                    private final String body = bodies[index.getAndIncrement()];

                    @Override
                    public HttpStatus getStatusCode() {
                        return HttpStatus.OK;
                    }

                    @Override
                    public String getStatusText() {
                        return "OK";
                    }

                    @Override
                    public void close() {
                    }

                    @Override
                    public InputStream getBody() {
                        return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
                    }

                    @Override
                    public HttpHeaders getHeaders() {
                        HttpHeaders headers = new HttpHeaders();
                        headers.add(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
                        return headers;
                    }
                });
        QqOAuthProviderClient client = new QqOAuthProviderClient(
                configuredProperties(),
                builder,
                new ObjectMapper()
        );

        OAuthProviderProfile profile = client.fetchProfile(
                "code-1",
                "http://localhost:8080/api/admin/auth/oauth/qq/callback"
        );

        assertThat(profile.provider()).isEqualTo("qq");
        assertThat(profile.providerUserId()).isEqualTo("openid-1");
        assertThat(profile.providerUsername()).isEqualTo("openid-1");
        assertThat(profile.nickname()).isEqualTo("Alice QQ");
        assertThat(profile.avatarUrl()).isEqualTo("https://q.qlogo.cn/2.png");
        assertThat(profile.email()).isNull();
        assertThat(profile.emailVerified()).isFalse();
    }

    private static QqOAuthProperties configuredProperties() {
        QqOAuthProperties properties = new QqOAuthProperties();
        properties.setAppId("appid 1");
        properties.setAppKey("appkey");
        return properties;
    }
}
