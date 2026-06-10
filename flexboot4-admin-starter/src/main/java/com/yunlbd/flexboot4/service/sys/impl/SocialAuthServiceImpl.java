package com.yunlbd.flexboot4.service.sys.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.dto.AuthLoginOptions;
import com.yunlbd.flexboot4.dto.LoginMethodOption;
import com.yunlbd.flexboot4.dto.LoginProviderOption;
import com.yunlbd.flexboot4.dto.LoginResp;
import com.yunlbd.flexboot4.dto.oauth.OAuthBindCandidate;
import com.yunlbd.flexboot4.dto.oauth.OAuthBindReq;
import com.yunlbd.flexboot4.dto.oauth.OAuthCallbackResultResp;
import com.yunlbd.flexboot4.dto.oauth.OAuthCallbackStatus;
import com.yunlbd.flexboot4.dto.oauth.OAuthProviderProfile;
import com.yunlbd.flexboot4.dto.oauth.OAuthUserSnapshot;
import com.yunlbd.flexboot4.dto.oauth.UserSocialAccountResp;
import com.yunlbd.flexboot4.entity.sys.SysUser;
import com.yunlbd.flexboot4.entity.sys.SysUserSocialAccount;
import com.yunlbd.flexboot4.oauth.OAuthProviderClient;
import com.yunlbd.flexboot4.service.sys.IAuthService;
import com.yunlbd.flexboot4.service.sys.SocialAuthService;
import com.yunlbd.flexboot4.service.sys.SysUserService;
import com.yunlbd.flexboot4.service.sys.SysUserSocialAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialAuthServiceImpl implements SocialAuthService {

    private static final String STATE_KEY_PREFIX = "auth:oauth:state:";
    private static final String RESULT_KEY_PREFIX = "auth:oauth:result:";
    private static final String BIND_KEY_PREFIX = "auth:oauth:bind:";
    private static final String LOGIN_TYPE_PREFIX = "oauth:";
    private static final String FRONTEND_CALLBACK_PATH = "/auth/oauth/callback";
    private static final long STATE_TTL_MINUTES = 5;
    private static final long RESULT_TTL_MINUTES = 3;
    private static final long BIND_TTL_MINUTES = 10;
    private static final int STATUS_ENABLED = 1;

    private final List<OAuthProviderClient> providerClients;
    private final IAuthService authService;
    private final SysUserService sysUserService;
    private final SysUserSocialAccountService socialAccountService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AuthenticationManager authenticationManager;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String buildAuthorizeUrl(String provider, HttpServletRequest request) {
        OAuthProviderClient client = requireEnabledProvider(provider);
        String normalizedProvider = normalizeProvider(provider);
        String state = randomToken();
        String redirectUri = client.callbackUri(buildRedirectUri(normalizedProvider, request));
        OAuthState statePayload = OAuthState.of(
                normalizedProvider,
                redirectUri,
                frontendCallbackBase(request)
        );
        setJson(STATE_KEY_PREFIX + state, statePayload, STATE_TTL_MINUTES);
        return client.buildAuthorizeUrl(state, statePayload.redirectUri());
    }

    @Override
    public String handleCallback(String provider, String code, String state, HttpServletRequest request) {
        String normalizedProvider = normalizeProvider(provider);
        if (!StringUtils.hasText(code)) {
            return buildFrontendCallbackUrl(frontendCallbackBase(request), errorResult("GitHub 授权失败或已取消"));
        }
        OAuthState statePayload = takeJson(STATE_KEY_PREFIX + state, OAuthState.class);
        if (statePayload == null || !normalizedProvider.equals(statePayload.provider())) {
            return buildFrontendCallbackUrl(frontendCallbackBase(request), errorResult("GitHub 授权状态已过期，请重新登录"));
        }

        try {
            OAuthProviderClient client = requireEnabledProvider(normalizedProvider);
            OAuthProviderProfile profile = client.fetchProfile(code, statePayload.redirectUri());
            OAuthCallbackResultResp result = buildResultForProfile(profile, request);
            return buildFrontendCallbackUrl(statePayload.frontendCallbackBase(), storeResult(result));
        } catch (Exception e) {
            log.warn("OAuth callback failed. provider={}, reason={}", normalizedProvider, e.getMessage());
            return buildFrontendCallbackUrl(statePayload.frontendCallbackBase(), errorResult(e.getMessage()));
        }
    }

    @Override
    public OAuthCallbackResultResp consumeResult(String ticket) {
        OAuthCallbackResultResp result = takeJson(RESULT_KEY_PREFIX + safeToken(ticket), OAuthCallbackResultResp.class);
        if (result == null) {
            return new OAuthCallbackResultResp(
                    OAuthCallbackStatus.ERROR,
                    null,
                    null,
                    null,
                    List.of(),
                    "登录结果已过期，请重新登录"
            );
        }
        return result;
    }

    @Override
    public LoginResp bind(String provider, OAuthBindReq req, String clientIp) {
        String normalizedProvider = normalizeProvider(provider);
        requireEnabledProvider(normalizedProvider);
        OAuthBindPayload payload = takeJson(BIND_KEY_PREFIX + safeToken(req.getBindTicket()), OAuthBindPayload.class);
        if (payload == null || !normalizedProvider.equals(payload.profile().provider())) {
            throw new IllegalStateException("绑定票据已过期，请重新登录");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );

        SysUser user = sysUserService.getOne(
                QueryWrapper.create()
                        .where(SysUser::getUsername).eq(req.getUsername())
                        .and(SysUser::getStatus).eq(STATUS_ENABLED)
                        .and(SysUser::getDelFlag).eq(0)
        );
        if (user == null) {
            throw new SecurityException("Invalid user");
        }

        ensureCanBind(user.getId(), payload.profile());
        saveSocialAccount(user.getId(), payload.profile(), true);
        return authService.loginVerifiedUser(user, LOGIN_TYPE_PREFIX + normalizedProvider, clientIp);
    }

    @Override
    public List<UserSocialAccountResp> listBoundAccounts(String userId) {
        if (!StringUtils.hasText(userId)) {
            return List.of();
        }
        return socialAccountService.list(
                        QueryWrapper.create()
                                .where(SysUserSocialAccount::getUserId).eq(userId)
                                .and(SysUserSocialAccount::getDelFlag).eq(0)
                )
                .stream()
                .map(account -> new UserSocialAccountResp(
                        account.getId(),
                        account.getProvider(),
                        account.getProviderUsername(),
                        account.getNickname(),
                        account.getAvatarUrl(),
                        maskEmail(account.getEmail()),
                        account.getEmailVerified(),
                        account.getBindTime(),
                        account.getLastLoginTime()
                ))
                .toList();
    }

    @Override
    public void unbind(String userId, String accountId) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(accountId)) {
            throw new IllegalArgumentException("绑定记录不存在");
        }
        SysUserSocialAccount account = socialAccountService.getById(accountId);
        if (account == null
                || (account.getDelFlag() != null && account.getDelFlag() != 0)
                || !userId.equals(account.getUserId())) {
            throw new IllegalArgumentException("绑定记录不存在");
        }
        account.setDelFlag(1);
        account.setLastModifyTime(LocalDateTime.now());
        socialAccountService.updateById(account, true);
    }

    private OAuthCallbackResultResp buildResultForProfile(OAuthProviderProfile profile, HttpServletRequest request) {
        SysUserSocialAccount account = findByProviderIdentity(profile.provider(), profile.providerUserId()).orElse(null);
        if (account != null) {
            if (account.getStatus() != null && account.getStatus() == 0) {
                throw new IllegalStateException("该第三方登录绑定已停用");
            }
            SysUser user = sysUserService.getById(account.getUserId());
            if (user == null || (user.getDelFlag() != null && user.getDelFlag() != 0)) {
                throw new SecurityException("Invalid user");
            }
            updateLastLogin(account, profile);
            LoginResp loginResp = authService.loginVerifiedUser(
                    user,
                    LOGIN_TYPE_PREFIX + profile.provider(),
                    com.yunlbd.flexboot4.security.JwtUtil.getClientIp(request)
            );
            OAuthCallbackStatus status = Boolean.TRUE.equals(loginResp.getMfaRequired())
                    ? OAuthCallbackStatus.MFA_REQUIRED
                    : OAuthCallbackStatus.LOGIN_SUCCESS;
            return new OAuthCallbackResultResp(status, loginResp, null, snapshot(profile), List.of(), null);
        }

        String bindTicket = randomToken();
        setJson(BIND_KEY_PREFIX + bindTicket, OAuthBindPayload.of(profile), BIND_TTL_MINUTES);
        return new OAuthCallbackResultResp(
                OAuthCallbackStatus.BIND_REQUIRED,
                null,
                bindTicket,
                snapshot(profile),
                findCandidates(profile),
                "请绑定已有系统账号"
        );
    }

    private void ensureCanBind(String userId, OAuthProviderProfile profile) {
        findByProviderIdentity(profile.provider(), profile.providerUserId()).ifPresent(existing -> {
            if (!userId.equals(existing.getUserId())) {
                throw new IllegalStateException("该 GitHub 账号已绑定其他系统账号");
            }
        });
        List<SysUserSocialAccount> userAccounts = socialAccountService.list(
                QueryWrapper.create()
                        .where(SysUserSocialAccount::getUserId).eq(userId)
                        .and(SysUserSocialAccount::getProvider).eq(profile.provider())
                        .and(SysUserSocialAccount::getDelFlag).eq(0)
        );
        for (SysUserSocialAccount account : userAccounts) {
            if (!Objects.equals(account.getProviderUserId(), profile.providerUserId())) {
                throw new IllegalStateException("当前系统账号已绑定其他 GitHub 账号，请先解绑");
            }
        }
    }

    private void saveSocialAccount(String userId, OAuthProviderProfile profile, boolean bindNow) {
        SysUserSocialAccount account = findByProviderIdentity(profile.provider(), profile.providerUserId()).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (account == null) {
            account = new SysUserSocialAccount();
            account.setUserId(userId);
            account.setProvider(profile.provider());
            account.setProviderUserId(profile.providerUserId());
            account.setBindTime(bindNow ? now : null);
            account.setCreateTime(now);
            account.setDelFlag(0);
            account.setVersion(0L);
        }
        applyProfile(account, profile);
        account.setUserId(userId);
        account.setStatus(STATUS_ENABLED);
        account.setLastLoginTime(now);
        if (account.getId() == null) {
            socialAccountService.save(account);
        } else {
            socialAccountService.updateById(account, true);
        }
    }

    private void updateLastLogin(SysUserSocialAccount account, OAuthProviderProfile profile) {
        applyProfile(account, profile);
        account.setLastLoginTime(LocalDateTime.now());
        socialAccountService.updateById(account, true);
    }

    private void applyProfile(SysUserSocialAccount account, OAuthProviderProfile profile) {
        account.setProviderUsername(profile.providerUsername());
        account.setNickname(profile.nickname());
        account.setAvatarUrl(profile.avatarUrl());
        account.setEmail(normalizeBlankToNull(profile.email()));
        account.setEmailVerified(profile.emailVerified());
    }

    private Optional<SysUserSocialAccount> findByProviderIdentity(String provider, String providerUserId) {
        if (!StringUtils.hasText(provider) || !StringUtils.hasText(providerUserId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(socialAccountService.getOne(
                QueryWrapper.create()
                        .where(SysUserSocialAccount::getProvider).eq(provider)
                        .and(SysUserSocialAccount::getProviderUserId).eq(providerUserId)
                        .and(SysUserSocialAccount::getDelFlag).eq(0)
        ));
    }

    private List<OAuthBindCandidate> findCandidates(OAuthProviderProfile profile) {
        if (!profile.emailVerified() || !StringUtils.hasText(profile.email())) {
            return List.of();
        }
        List<SysUser> users = sysUserService.list(
                QueryWrapper.create()
                        .where("lower(btrim(email)) = ?", profile.email().trim().toLowerCase())
                        .and(SysUser::getStatus).eq(STATUS_ENABLED)
                        .and(SysUser::getDelFlag).eq(0)
        );
        return users.stream()
                .map(user -> new OAuthBindCandidate(
                        user.getId(),
                        user.getUsername(),
                        user.getRealName(),
                        maskEmail(user.getEmail())
                ))
                .toList();
    }

    private OAuthProviderClient requireEnabledProvider(String provider) {
        String normalized = normalizeProvider(provider);
        LoginMethodOption thirdParty = authService.getLoginOptions().method(AuthLoginOptions.METHOD_THIRD_PARTY);
        boolean providerEnabled = thirdParty.getProviders() != null && thirdParty.getProviders().stream()
                .filter(option -> option != null && normalized.equalsIgnoreCase(option.getCode()))
                .findFirst()
                .map(option -> option.isEnabled(false))
                .orElse(false);
        if (!providerEnabled) {
            throw new IllegalStateException("GitHub 登录未启用");
        }
        OAuthProviderClient client = providerMap().get(normalized);
        if (client == null) {
            throw new IllegalArgumentException("不支持的第三方登录方式");
        }
        if (!client.configured()) {
            throw new IllegalStateException("GitHub 登录未配置 clientId/clientSecret");
        }
        return client;
    }

    private Map<String, OAuthProviderClient> providerMap() {
        return providerClients.stream()
                .collect(Collectors.toMap(
                        client -> normalizeProvider(client.provider()),
                        Function.identity(),
                        (left, ignored) -> left
                ));
    }

    private String storeResult(OAuthCallbackResultResp result) {
        String ticket = randomToken();
        setJson(RESULT_KEY_PREFIX + ticket, result, RESULT_TTL_MINUTES);
        return ticket;
    }

    private String errorResult(String message) {
        return storeResult(new OAuthCallbackResultResp(
                OAuthCallbackStatus.ERROR,
                null,
                null,
                null,
                List.of(),
                StringUtils.hasText(message) ? message : "第三方登录失败"
        ));
    }

    private String buildFrontendCallbackUrl(String frontendCallbackBase, String ticket) {
        return UriComponentsBuilder.fromUriString(frontendCallbackBase)
                .queryParam("ticket", ticket)
                .build()
                .toUriString();
    }

    private String buildRedirectUri(String provider, HttpServletRequest request) {
        return requestOrigin(request)
                + request.getContextPath()
                + "/api/admin/auth/oauth/"
                + provider
                + "/callback";
    }

    private String frontendCallbackBase(HttpServletRequest request) {
        return frontendOrigin(request) + FRONTEND_CALLBACK_PATH;
    }

    private String frontendOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (StringUtils.hasText(origin)) {
            return trimTrailingSlash(origin);
        }
        String referer = request.getHeader("Referer");
        if (StringUtils.hasText(referer)) {
            try {
                URI uri = URI.create(referer);
                return uri.getScheme() + "://" + uri.getAuthority();
            } catch (Exception ignored) {
            }
        }
        return requestOrigin(request);
    }

    private String requestOrigin(HttpServletRequest request) {
        String forwardedProto = firstHeaderValue(request.getHeader("X-Forwarded-Proto"));
        String forwardedHost = firstHeaderValue(request.getHeader("X-Forwarded-Host"));
        String scheme = StringUtils.hasText(forwardedProto) ? forwardedProto : request.getScheme();
        String host = StringUtils.hasText(forwardedHost) ? forwardedHost : request.getHeader("Host");
        if (!StringUtils.hasText(host)) {
            host = request.getServerName() + ":" + request.getServerPort();
        }
        return scheme + "://" + host;
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void setJson(String key, Object payload, long ttlMinutes) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(payload), ttlMinutes, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("OAuth 状态写入失败", e);
        }
    }

    private <T> T takeJson(String key, Class<T> type) {
        String raw = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, type);
        } catch (Exception e) {
            return null;
        }
    }

    private static OAuthUserSnapshot snapshot(OAuthProviderProfile profile) {
        return new OAuthUserSnapshot(
                profile.provider(),
                profile.providerUsername(),
                profile.nickname(),
                profile.avatarUrl(),
                profile.email(),
                profile.emailVerified()
        );
    }

    private static String normalizeProvider(String provider) {
        if (!StringUtils.hasText(provider)) {
            throw new IllegalArgumentException("第三方登录方式不能为空");
        }
        return provider.trim().toLowerCase();
    }

    private static String safeToken(String token) {
        if (!StringUtils.hasText(token)) {
            return "";
        }
        return token.trim();
    }

    private static String trimTrailingSlash(String value) {
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String firstHeaderValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.split(",")[0].trim();
    }

    private static String normalizeBlankToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static String maskEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return email;
        }
        int visibleLength = at > 3 ? 3 : 1;
        return email.substring(0, visibleLength) + "***" + email.substring(at);
    }

    public static class OAuthState {
        private String provider;
        private String redirectUri;
        private String frontendCallbackBase;

        public static OAuthState of(String provider, String redirectUri, String frontendCallbackBase) {
            OAuthState state = new OAuthState();
            state.provider = provider;
            state.redirectUri = redirectUri;
            state.frontendCallbackBase = frontendCallbackBase;
            return state;
        }

        public String provider() {
            return provider;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String redirectUri() {
            return redirectUri;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        public String frontendCallbackBase() {
            return frontendCallbackBase;
        }

        public String getFrontendCallbackBase() {
            return frontendCallbackBase;
        }

        public void setFrontendCallbackBase(String frontendCallbackBase) {
            this.frontendCallbackBase = frontendCallbackBase;
        }
    }

    public static class OAuthBindPayload {
        private OAuthProviderProfile profile;

        public static OAuthBindPayload of(OAuthProviderProfile profile) {
            OAuthBindPayload payload = new OAuthBindPayload();
            payload.profile = profile;
            return payload;
        }

        public OAuthProviderProfile profile() {
            return profile;
        }

        public OAuthProviderProfile getProfile() {
            return profile;
        }

        public void setProfile(OAuthProviderProfile profile) {
            this.profile = profile;
        }
    }
}
