package com.yunlbd.flexboot4.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunlbd.flexboot4.dto.oauth.OAuthProviderProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class QqOAuthProviderClient implements OAuthProviderClient {

    private static final String PROVIDER = "qq";

    private final QqOAuthProperties properties;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public boolean configured() {
        return StringUtils.hasText(properties.getAppId())
                && StringUtils.hasText(properties.getAppKey());
    }

    @Override
    public String callbackUri(String fallback) {
        return StringUtils.hasText(properties.getCallbackUri())
                ? properties.getCallbackUri()
                : fallback;
    }

    @Override
    public String buildAuthorizeUrl(String state, String redirectUri) {
        ensureConfigured();
        return UriComponentsBuilder.fromUriString(properties.getAuthorizeUrl())
                .queryParam("response_type", "code")
                .queryParam("client_id", properties.getAppId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", properties.getScope())
                .queryParam("state", state)
                .encode()
                .build()
                .toUriString();
    }

    @Override
    public OAuthProviderProfile fetchProfile(String code, String redirectUri) {
        ensureConfigured();
        RestClient restClient = restClientBuilder.build();
        String tokenRaw = restClient.get()
                .uri(UriComponentsBuilder.fromUriString(properties.getTokenUrl())
                        .queryParam("grant_type", "authorization_code")
                        .queryParam("client_id", properties.getAppId())
                        .queryParam("client_secret", properties.getAppKey())
                        .queryParam("code", code)
                        .queryParam("redirect_uri", redirectUri)
                        .encode()
                        .build()
                        .toUri())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);
        String accessToken = parseAccessToken(tokenRaw);
        if (!StringUtils.hasText(accessToken)) {
            throw new IllegalStateException("QQ 授权失败，未获取到 access token");
        }

        String openidRaw = restClient.get()
                .uri(UriComponentsBuilder.fromUriString(properties.getOpenidUrl())
                        .queryParam("access_token", accessToken)
                        .queryParam("fmt", "json")
                        .encode()
                        .build()
                        .toUri())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);
        String openid = parseOpenid(openidRaw);
        if (!StringUtils.hasText(openid)) {
            throw new IllegalStateException("QQ OpenID 获取失败");
        }

        QqUserInfoResp userInfo = restClient.get()
                .uri(UriComponentsBuilder.fromUriString(properties.getUserInfoUrl())
                        .queryParam("access_token", accessToken)
                        .queryParam("oauth_consumer_key", properties.getAppId())
                        .queryParam("openid", openid)
                        .encode()
                        .build()
                        .toUri())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(QqUserInfoResp.class);
        if (userInfo == null || userInfo.ret() == null || userInfo.ret() != 0) {
            throw new IllegalStateException("QQ 用户信息获取失败");
        }

        return new OAuthProviderProfile(
                PROVIDER,
                openid,
                openid,
                firstNonBlank(userInfo.nickname(), openid),
                firstNonBlank(
                        userInfo.figureurlQq2(),
                        userInfo.figureurlQq1(),
                        userInfo.figureurl2(),
                        userInfo.figureurl1()
                ),
                null,
                false
        );
    }

    String parseAccessToken(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String normalized = raw.trim();
        if (normalized.startsWith("{")) {
            try {
                Map<?, ?> resp = objectMapper.readValue(normalized, Map.class);
                return asString(resp.get("access_token"));
            } catch (Exception ignored) {
                return null;
            }
        }
        return parseForm(normalized).get("access_token");
    }

    String parseOpenid(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String normalized = raw.trim();
        if (normalized.startsWith("callback(") && normalized.endsWith(");")) {
            normalized = normalized.substring("callback(".length(), normalized.length() - 2).trim();
        } else if (normalized.startsWith("callback(") && normalized.endsWith(")")) {
            normalized = normalized.substring("callback(".length(), normalized.length() - 1).trim();
        }
        try {
            Map<?, ?> resp = objectMapper.readValue(normalized, Map.class);
            return asString(resp.get("openid"));
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, String> parseForm(String raw) {
        Map<String, String> values = new HashMap<>();
        for (String part : raw.split("&")) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            String[] pair = part.split("=", 2);
            String key = decode(pair[0]);
            String value = pair.length > 1 ? decode(pair[1]) : "";
            values.put(key, value);
        }
        return values;
    }

    private void ensureConfigured() {
        if (!configured()) {
            throw new IllegalStateException("QQ 登录未配置 appId/appKey");
        }
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private record QqUserInfoResp(
            Integer ret,
            String msg,
            String nickname,
            @JsonProperty("figureurl_1") String figureurl1,
            @JsonProperty("figureurl_2") String figureurl2,
            @JsonProperty("figureurl_qq_1") String figureurlQq1,
            @JsonProperty("figureurl_qq_2") String figureurlQq2
    ) {
    }
}
