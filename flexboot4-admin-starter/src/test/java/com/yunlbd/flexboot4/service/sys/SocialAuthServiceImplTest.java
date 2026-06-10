package com.yunlbd.flexboot4.service.sys;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.dto.AuthLoginOptions;
import com.yunlbd.flexboot4.dto.LoginProviderOption;
import com.yunlbd.flexboot4.dto.LoginResp;
import com.yunlbd.flexboot4.dto.oauth.OAuthBindReq;
import com.yunlbd.flexboot4.dto.oauth.OAuthCallbackResultResp;
import com.yunlbd.flexboot4.dto.oauth.OAuthCallbackStatus;
import com.yunlbd.flexboot4.dto.oauth.OAuthProviderProfile;
import com.yunlbd.flexboot4.entity.sys.SysUser;
import com.yunlbd.flexboot4.entity.sys.SysUserSocialAccount;
import com.yunlbd.flexboot4.oauth.OAuthProviderClient;
import com.yunlbd.flexboot4.service.sys.impl.SocialAuthServiceImpl;
import com.yunlbd.flexboot4.service.sys.impl.SocialAuthServiceImpl.OAuthBindPayload;
import com.yunlbd.flexboot4.service.sys.impl.SocialAuthServiceImpl.OAuthState;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SocialAuthServiceImplTest {

    private final IAuthService authService = mock(IAuthService.class);
    private final SysUserService sysUserService = mock(SysUserService.class);
    private final SysUserSocialAccountService socialAccountService = mock(SysUserSocialAccountService.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final FakeProviderClient providerClient = new FakeProviderClient();
    private final FakeProviderClient qqProviderClient = new FakeProviderClient("qq", "qq-openid", null, false);
    private SocialAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(authService.getLoginOptions()).thenReturn(enabledGithubOptions());
        service = new SocialAuthServiceImpl(
                List.of(providerClient, qqProviderClient),
                authService,
                sysUserService,
                socialAccountService,
                redisTemplate,
                objectMapper,
                authenticationManager
        );
    }

    @Test
    void buildAuthorizeUrlStoresStateAndRedirectUri() {
        String url = service.buildAuthorizeUrl("github", request());

        assertThat(url).startsWith("https://github.test/authorize?");
        ArgumentCaptor<String> stateCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                stateCaptor.capture(),
                any(String.class),
                eq(5L),
                eq(TimeUnit.MINUTES)
        );
        assertThat(stateCaptor.getValue()).startsWith("auth:oauth:state:");
    }

    @Test
    void callbackForUnboundProfileReturnsBindRequiredWithCandidate() throws Exception {
        String stateJson = objectMapper.writeValueAsString(OAuthState.of(
                "github",
                "http://localhost/api/admin/auth/oauth/github/callback",
                "http://localhost/auth/oauth/callback"
        ));
        when(valueOperations.get("auth:oauth:state:state1")).thenReturn(stateJson);
        when(socialAccountService.getOne(any(QueryWrapper.class))).thenReturn(null);
        SysUser candidate = activeUser();
        candidate.setEmail("alice@example.com");
        when(sysUserService.list(any(QueryWrapper.class))).thenReturn(List.of(candidate));

        String redirect = service.handleCallback("github", "code1", "state1", request());

        assertThat(redirect).startsWith("http://localhost/auth/oauth/callback?ticket=");
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.startsWith("auth:oauth:bind:"),
                any(String.class),
                eq(10L),
                eq(TimeUnit.MINUTES)
        );
        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.startsWith("auth:oauth:result:"),
                payloadCaptor.capture(),
                eq(3L),
                eq(TimeUnit.MINUTES)
        );
        OAuthCallbackResultResp result = objectMapper.readValue(payloadCaptor.getValue(), OAuthCallbackResultResp.class);
        assertThat(result.status()).isEqualTo(OAuthCallbackStatus.BIND_REQUIRED);
        assertThat(result.candidates()).hasSize(1);
        assertThat(result.candidates().getFirst().username()).isEqualTo("alice");
    }

    @Test
    void callbackForBoundProfileReturnsLoginResult() throws Exception {
        String stateJson = objectMapper.writeValueAsString(OAuthState.of(
                "github",
                "http://localhost/api/admin/auth/oauth/github/callback",
                "http://localhost/auth/oauth/callback"
        ));
        when(valueOperations.get("auth:oauth:state:state1")).thenReturn(stateJson);
        SysUserSocialAccount account = boundAccount("u1", "gh1");
        when(socialAccountService.getOne(any(QueryWrapper.class))).thenReturn(account);
        when(sysUserService.getById("u1")).thenReturn(activeUser());
        LoginResp loginResp = new LoginResp();
        loginResp.setAccessToken("jwt");
        loginResp.setMfaRequired(false);
        when(authService.loginVerifiedUser(any(SysUser.class), eq("oauth:github"), eq("127.0.0.1"))).thenReturn(loginResp);

        service.handleCallback("github", "code1", "state1", request());

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.startsWith("auth:oauth:result:"),
                payloadCaptor.capture(),
                eq(3L),
                eq(TimeUnit.MINUTES)
        );
        OAuthCallbackResultResp result = objectMapper.readValue(payloadCaptor.getValue(), OAuthCallbackResultResp.class);
        assertThat(result.status()).isEqualTo(OAuthCallbackStatus.LOGIN_SUCCESS);
        assertThat(result.login().getAccessToken()).isEqualTo("jwt");
        verify(socialAccountService).updateById(eq(account), eq(true));
    }

    @Test
    void bindRejectsWhenLocalUserAlreadyBoundToDifferentGithubAccount() throws Exception {
        OAuthProviderProfile profile = providerClient.profile;
        when(valueOperations.get("auth:oauth:bind:bind1")).thenReturn(objectMapper.writeValueAsString(OAuthBindPayload.of(profile)));
        when(sysUserService.getOne(any(QueryWrapper.class))).thenReturn(activeUser());
        when(socialAccountService.getOne(any(QueryWrapper.class))).thenReturn(null);
        when(socialAccountService.list(any(QueryWrapper.class))).thenReturn(List.of(boundAccount("u1", "other-gh")));

        OAuthBindReq req = new OAuthBindReq();
        req.setBindTicket("bind1");
        req.setUsername("alice");
        req.setPassword("11111111");

        assertThatThrownBy(() -> service.bind("github", req, "127.0.0.1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("已绑定其他 GitHub 账号");
    }

    @Test
    void disabledProviderRejectsAuthorize() {
        AuthLoginOptions options = AuthLoginOptions.defaults();
        options.getMethods().get(AuthLoginOptions.METHOD_THIRD_PARTY).setProviders(List.of(LoginProviderOption.github(false)));
        when(authService.getLoginOptions()).thenReturn(options);

        assertThatThrownBy(() -> service.buildAuthorizeUrl("github", request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("GitHub 登录未启用");
    }

    @Test
    void providerEnabledControlsAuthorizeWithoutThirdPartySwitch() {
        AuthLoginOptions options = enabledGithubOptions();
        options.getMethods().get(AuthLoginOptions.METHOD_THIRD_PARTY).setEnabled(false);
        when(authService.getLoginOptions()).thenReturn(options);

        String url = service.buildAuthorizeUrl("github", request());

        assertThat(url).startsWith("https://github.test/authorize?");
    }

    @Test
    void callbackForUnboundQqProfileReturnsBindRequiredWithoutCandidate() throws Exception {
        when(authService.getLoginOptions()).thenReturn(enabledOptions("qq", true));
        String stateJson = objectMapper.writeValueAsString(OAuthState.of(
                "qq",
                "http://localhost/api/admin/auth/oauth/qq/callback",
                "http://localhost/auth/oauth/callback"
        ));
        when(valueOperations.get("auth:oauth:state:state1")).thenReturn(stateJson);
        when(socialAccountService.getOne(any(QueryWrapper.class))).thenReturn(null);

        service.handleCallback("qq", "code1", "state1", request());

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.startsWith("auth:oauth:result:"),
                payloadCaptor.capture(),
                eq(3L),
                eq(TimeUnit.MINUTES)
        );
        OAuthCallbackResultResp result = objectMapper.readValue(payloadCaptor.getValue(), OAuthCallbackResultResp.class);
        assertThat(result.status()).isEqualTo(OAuthCallbackStatus.BIND_REQUIRED);
        assertThat(result.externalUser().provider()).isEqualTo("qq");
        assertThat(result.candidates()).isEmpty();
    }

    @Test
    void qqDisabledProviderRejectsAuthorizeWithProviderName() {
        when(authService.getLoginOptions()).thenReturn(enabledOptions("qq", false));

        assertThatThrownBy(() -> service.buildAuthorizeUrl("qq", request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("QQ 登录未启用");
    }

    @Test
    void qqUnconfiguredProviderRejectsAuthorizeWithProviderName() {
        qqProviderClient.configured = false;
        when(authService.getLoginOptions()).thenReturn(enabledOptions("qq", true));

        assertThatThrownBy(() -> service.buildAuthorizeUrl("qq", request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("QQ 登录未配置");
    }

    private static AuthLoginOptions enabledGithubOptions() {
        return enabledOptions("github", true);
    }

    private static AuthLoginOptions enabledOptions(String provider, boolean enabled) {
        AuthLoginOptions options = AuthLoginOptions.defaults();
        options.getMethods().get(AuthLoginOptions.METHOD_THIRD_PARTY)
                .setProviders(List.of(LoginProviderOption.of(provider, enabled)));
        return options;
    }

    private static HttpServletRequest request() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Host")).thenReturn("localhost");
        when(request.getHeader("Origin")).thenReturn("http://localhost");
        when(request.getHeader("X-Forwarded-Proto")).thenReturn(null);
        when(request.getHeader("X-Forwarded-Host")).thenReturn(null);
        when(request.getContextPath()).thenReturn("");
        when(request.getScheme()).thenReturn("http");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        return request;
    }

    private static SysUser activeUser() {
        SysUser user = new SysUser();
        user.setId("u1");
        user.setUsername("alice");
        user.setRealName("Alice");
        user.setStatus(1);
        user.setDelFlag(0);
        return user;
    }

    private static SysUserSocialAccount boundAccount(String userId, String providerUserId) {
        SysUserSocialAccount account = new SysUserSocialAccount();
        account.setId("sa1");
        account.setUserId(userId);
        account.setProvider("github");
        account.setProviderUserId(providerUserId);
        account.setStatus(1);
        account.setDelFlag(0);
        return account;
    }

    private static class FakeProviderClient implements OAuthProviderClient {
        private final String provider;
        private final OAuthProviderProfile profile;
        private boolean configured = true;

        private FakeProviderClient() {
            this("github", "gh1", "alice@example.com", true);
        }

        private FakeProviderClient(String provider, String providerUserId, String email, boolean emailVerified) {
            this.provider = provider;
            this.profile = new OAuthProviderProfile(
                    provider,
                    providerUserId,
                    "alice-" + provider,
                    "Alice " + provider,
                    "https://avatars.example/alice.png",
                    email,
                    emailVerified
            );
        }

        @Override
        public String provider() {
            return provider;
        }

        @Override
        public boolean configured() {
            return configured;
        }

        @Override
        public String buildAuthorizeUrl(String state, String redirectUri) {
            return "https://" + provider + ".test/authorize?state=" + state + "&redirect_uri=" + redirectUri;
        }

        @Override
        public OAuthProviderProfile fetchProfile(String code, String redirectUri) {
            return profile;
        }
    }
}
