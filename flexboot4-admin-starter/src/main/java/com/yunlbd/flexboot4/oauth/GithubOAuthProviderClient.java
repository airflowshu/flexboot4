package com.yunlbd.flexboot4.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yunlbd.flexboot4.dto.oauth.OAuthProviderProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GithubOAuthProviderClient implements OAuthProviderClient {

    private static final String PROVIDER = "github";

    private final GithubOAuthProperties properties;
    private final RestClient.Builder restClientBuilder;

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public boolean configured() {
        return StringUtils.hasText(properties.getClientId())
                && StringUtils.hasText(properties.getClientSecret());
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
                .queryParam("client_id", properties.getClientId())
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
        GithubTokenResp tokenResp = restClient.post()
                .uri(properties.getTokenUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "client_id", properties.getClientId(),
                        "client_secret", properties.getClientSecret(),
                        "code", code,
                        "redirect_uri", redirectUri
                ))
                .retrieve()
                .body(GithubTokenResp.class);

        if (tokenResp == null || !StringUtils.hasText(tokenResp.accessToken())) {
            throw new IllegalStateException("GitHub 授权失败，未获取到 access token");
        }

        GithubUserResp userResp = restClient.get()
                .uri(properties.getUserUrl())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenResp.accessToken())
                .retrieve()
                .body(GithubUserResp.class);

        if (userResp == null || userResp.id() == null) {
            throw new IllegalStateException("GitHub 用户信息获取失败");
        }

        GithubEmailResp email = fetchVerifiedEmail(restClient, tokenResp.accessToken());
        String emailValue = email == null ? userResp.email() : email.email();
        boolean emailVerified = email != null && Boolean.TRUE.equals(email.verified());

        return new OAuthProviderProfile(
                PROVIDER,
                String.valueOf(userResp.id()),
                userResp.login(),
                firstNonBlank(userResp.name(), userResp.login()),
                userResp.avatarUrl(),
                emailValue,
                emailVerified
        );
    }

    private GithubEmailResp fetchVerifiedEmail(RestClient restClient, String accessToken) {
        try {
            GithubEmailResp[] emails = restClient.get()
                    .uri(properties.getEmailsUrl())
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(GithubEmailResp[].class);
            if (emails == null || emails.length == 0) {
                return null;
            }
            return List.of(emails).stream()
                    .filter(email -> email.email() != null && Boolean.TRUE.equals(email.verified()))
                    .max(Comparator.comparing((GithubEmailResp email) -> Boolean.TRUE.equals(email.primary())))
                    .orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void ensureConfigured() {
        if (!configured()) {
            throw new IllegalStateException("GitHub 登录未配置 clientId/clientSecret");
        }
    }

    private static String firstNonBlank(String first, String fallback) {
        return StringUtils.hasText(first) ? first : fallback;
    }

    private record GithubTokenResp(@JsonProperty("access_token") String accessToken) {
    }

    private record GithubUserResp(
            Long id,
            String login,
            String name,
            String email,
            @JsonProperty("avatar_url") String avatarUrl
    ) {
    }

    private record GithubEmailResp(
            String email,
            Boolean primary,
            Boolean verified
    ) {
    }
}
