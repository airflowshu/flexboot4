package com.yunlbd.flexboot4.service.sys.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.dto.AdminResetPasswordReq;
import com.yunlbd.flexboot4.dto.AuthLoginOptions;
import com.yunlbd.flexboot4.dto.ForgetPasswordReq;
import com.yunlbd.flexboot4.dto.LoginMethodOption;
import com.yunlbd.flexboot4.dto.LoginReq;
import com.yunlbd.flexboot4.dto.LoginResp;
import com.yunlbd.flexboot4.dto.MfaVerifyReq;
import com.yunlbd.flexboot4.dto.ResetPasswordReq;
import com.yunlbd.flexboot4.dto.SmsCodeReq;
import com.yunlbd.flexboot4.entity.sys.SysRole;
import com.yunlbd.flexboot4.entity.sys.SysUser;
import com.yunlbd.flexboot4.metrics.MetricsRecorder;
import com.yunlbd.flexboot4.security.JwtUtil;
import com.yunlbd.flexboot4.security.UserDetailsCacheService;
import com.yunlbd.flexboot4.service.sys.ConfigLookupService;
import com.yunlbd.flexboot4.service.sys.EmailService;
import com.yunlbd.flexboot4.service.sys.IAuthService;
import com.yunlbd.flexboot4.service.sys.SysMenuService;
import com.yunlbd.flexboot4.service.sys.SysUserService;
import com.yunlbd.flexboot4.service.sys.UserMfaService;
import com.yunlbd.flexboot4.sms.SmsMessageRequest;
import com.yunlbd.flexboot4.sms.SmsMessageSender;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private static final String LOGIN_LIMIT_KEY_PREFIX = "auth:limit:";
    private static final String BLACKLIST_KEY_PREFIX = "auth:blacklist:";
    private static final String SMS_CODE_KEY_PREFIX = "auth:sms-code:";
    private static final String SMS_COOLDOWN_KEY_PREFIX = "auth:sms-code:cooldown:";
    private static final String SMS_DAILY_KEY_PREFIX = "auth:sms-code:daily:";
    private static final String SMS_FAIL_KEY_PREFIX = "auth:sms-code:fail:";
    private static final String SMS_IP_HOUR_KEY_PREFIX = "auth:sms-code:ip:hour:";
    private static final String SMS_IP_DAILY_KEY_PREFIX = "auth:sms-code:ip:daily:";
    private static final String MFA_CHALLENGE_KEY_PREFIX = "auth:mfa:challenge:";
    private static final String LOGIN_TYPE_PASSWORD = "password";
    private static final String LOGIN_TYPE_SMS = "sms";
    private static final String LOGIN_OPTIONS_KEY = "auth.login.options";
    private static final String SMS_CONFIG_ID_KEY = "auth.sms.configId";
    private static final String SMS_TEMPLATE_ID_KEY = "auth.sms.templateId";
    private static final String SMS_IP_HOURLY_LIMIT_KEY = "auth.sms.ipHourlyLimit";
    private static final String SMS_IP_DAILY_LIMIT_KEY = "auth.sms.ipDailyLimit";
    private static final String SMS_SEND_SUCCESS_MSG = "验证码已发送，请注意查收";
    private static final String SMS_CODE_INVALID_MSG = "验证码不正确或已过期";
    private static final String SMS_SEND_TOO_FREQUENT_MSG = "验证码发送过于频繁，请稍后再试";
    private static final String SMS_SEND_DAILY_LIMIT_MSG = "验证码发送次数已达今日上限";
    private static final int DEFAULT_SMS_CODE_LENGTH = 6;
    private static final int DEFAULT_SMS_TTL_MINUTES = 5;
    private static final int DEFAULT_SMS_COOLDOWN_SECONDS = 60;
    private static final int DEFAULT_SMS_DAILY_LIMIT = 10;
    private static final int DEFAULT_SMS_IP_HOURLY_LIMIT = 30;
    private static final int DEFAULT_SMS_IP_DAILY_LIMIT = 100;
    private static final int MAX_SMS_VERIFY_ATTEMPTS = 5;
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int MAX_MFA_VERIFY_ATTEMPTS = 5;
    private static final long LOCK_TIME_MINUTES = 15;
    private static final long SMS_IP_HOURLY_TTL_MINUTES = 60;
    private static final long MFA_CHALLENGE_TTL_MINUTES = 5;
    private static final long MAX_DAILY_TTL_MINUTES = 1440;
    private static final String RESET_LINK_SENT_MSG = "Reset link sent. Please check your email.";

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final SysMenuService sysMenuService;
    private final UserDetailsCacheService userDetailsCacheService;
    private final ObjectProvider<EmailService> emailServiceProvider;
    private final ObjectProvider<SmsMessageSender> smsMessageSenderProvider;
    private final ConfigLookupService configLookupService;
    private final SysUserService sysUserService;
    private final UserMfaService userMfaService;
    private final MetricsRecorder metricsRecorder;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public AuthLoginOptions getLoginOptions() {
        String raw = configLookupService.getConfigValue(LOGIN_OPTIONS_KEY);
        if (raw == null || raw.isBlank()) {
            return AuthLoginOptions.defaults();
        }
        try {
            return objectMapper.readValue(raw, AuthLoginOptions.class).mergeDefaults();
        } catch (JsonProcessingException e) {
            log.warn("Invalid auth login options config, fallback to defaults: {}", e.getMessage());
            return AuthLoginOptions.defaults();
        }
    }

    @Override
    public String sendSmsCode(SmsCodeReq req, String clientIp) {
        LoginMethodOption smsOption = getLoginOptions().method(AuthLoginOptions.METHOD_SMS);
        if (smsOption == null || !smsOption.isEnabled(false)) {
            throw new IllegalStateException("短信验证码登录未启用");
        }

        String phone = normalizePhone(req.getPhone());
        int codeLength = optionInt(smsOption.getCodeLength(), DEFAULT_SMS_CODE_LENGTH);
        int cooldownSeconds = optionInt(smsOption.getCooldownSeconds(), DEFAULT_SMS_COOLDOWN_SECONDS);

        if (!isValidPhone(phone)) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
        enforceSmsIpRateLimit(clientIp);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(SMS_COOLDOWN_KEY_PREFIX + phone))) {
            throw new IllegalStateException(SMS_SEND_TOO_FREQUENT_MSG);
        }

        String dailyKey = SMS_DAILY_KEY_PREFIX + phone;
        long dailyCount = incrementDailyCounter(dailyKey);
        if (dailyCount > DEFAULT_SMS_DAILY_LIMIT) {
            metricsRecorder.increment("flexboot4.auth.sms_send_limited", Map.of("clientIp", clientIp));
            throw new IllegalStateException(SMS_SEND_DAILY_LIMIT_MSG);
        }

        SysUser user = findActiveUserByPhone(phone);
        if (user == null) {
            log.info("SMS login code requested for unmatched phone: {}", maskPhone(phone));
            metricsRecorder.increment("flexboot4.auth.sms_code_skip", Map.of("reason", "user_not_found", "clientIp", clientIp));
            redisTemplate.opsForValue().set(SMS_COOLDOWN_KEY_PREFIX + phone, "1", cooldownSeconds, TimeUnit.SECONDS);
            return SMS_SEND_SUCCESS_MSG;
        }

        String code = randomNumericCode(codeLength);
        String codeKey = SMS_CODE_KEY_PREFIX + phone;
        String failKey = SMS_FAIL_KEY_PREFIX + phone;
        redisTemplate.opsForValue().set(codeKey, hashSmsCode(phone, code), DEFAULT_SMS_TTL_MINUTES, TimeUnit.MINUTES);
        redisTemplate.delete(failKey);
        redisTemplate.opsForValue().set(SMS_COOLDOWN_KEY_PREFIX + phone, "1", cooldownSeconds, TimeUnit.SECONDS);

        SmsMessageSender sender = smsMessageSenderProvider.getIfAvailable();
        if (sender == null) {
            log.warn("SMS login code generated but no SmsMessageSender bean is available. phone={}", maskPhone(phone));
            metricsRecorder.increment("flexboot4.auth.sms_sender_missing", Map.of("clientIp", clientIp));
            return SMS_SEND_SUCCESS_MSG;
        }

        try {
            sender.send(new SmsMessageRequest(
                    phone,
                    configLookupService.getConfigValue(SMS_TEMPLATE_ID_KEY),
                    smsTemplateParams(code),
                    configLookupService.getConfigValue(SMS_CONFIG_ID_KEY)
            ));
            metricsRecorder.increment("flexboot4.auth.sms_code_sent", Map.of("clientIp", clientIp));
            return SMS_SEND_SUCCESS_MSG;
        } catch (Exception e) {
            redisTemplate.delete(codeKey);
            redisTemplate.delete(failKey);
            log.warn("Failed to send SMS login code. phone={}, reason={}", maskPhone(phone), e.getMessage());
            metricsRecorder.increment("flexboot4.auth.sms_send_failed", Map.of(
                    "clientIp", clientIp,
                    "exception", e.getClass().getSimpleName()
            ));
            throw new IllegalStateException("验证码发送失败，请稍后再试");
        }
    }

    @Override
    public List<String> getPermissionCodes(HttpServletRequest request) {
        String token = jwtUtil.resolveToken(request);
        if (token == null) {
            return Collections.emptyList();
        }
        String userId = jwtUtil.extractUserId(token);
        return sysMenuService.getPermissionCodes(userId);
    }

    @Override
    public LoginResp login(LoginReq req, String clientIp) {
        String loginType = req.getLoginType() == null || req.getLoginType().isBlank()
                ? LOGIN_TYPE_PASSWORD
                : req.getLoginType().trim().toLowerCase();
        return switch (loginType) {
            case LOGIN_TYPE_SMS -> smsLogin(req, clientIp);
            case LOGIN_TYPE_PASSWORD -> passwordLogin(req, clientIp);
            default -> throw new IllegalArgumentException("不支持的登录方式");
        };
    }

    @Override
    public LoginResp loginVerifiedUser(SysUser sysUser, String loginType, String clientIp) {
        if (sysUser == null || sysUser.getUsername() == null || sysUser.getUsername().isBlank()) {
            throw new SecurityException("Invalid user");
        }
        if (sysUser.getStatus() != null && sysUser.getStatus() == 0) {
            throw new IllegalStateException("当前账号已停用");
        }
        if (sysUser.getDelFlag() != null && sysUser.getDelFlag() != 0) {
            throw new SecurityException("Invalid user");
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(sysUser.getUsername());
        return buildLoginResult(sysUser, userDetails, loginType, clientIp);
    }

    private LoginResp passwordLogin(LoginReq req, String clientIp) {
        LoginMethodOption passwordOption = getLoginOptions().method(AuthLoginOptions.METHOD_PASSWORD);
        if (passwordOption == null || !passwordOption.isEnabled(true)) {
            throw new IllegalStateException("账号密码登录未启用");
        }

        String limitKey = LOGIN_LIMIT_KEY_PREFIX + req.getUsername() + ":" + clientIp;

        String attemptsStr = redisTemplate.opsForValue().get(limitKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;
        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            log.warn("Login locked for user: {} IP: {}", req.getUsername(), clientIp);
            metricsRecorder.increment("flexboot4.auth.login_locked", Map.of("clientIp", clientIp));
            throw new SecurityException("Too many login attempts. Please try again later.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            SysUser sysUser = sysUserService.getOne(
                    QueryWrapper.create().where(SysUser::getUsername).eq(req.getUsername())
            );

            redisTemplate.delete(limitKey);
            metricsRecorder.increment("flexboot4.auth.login_success", Map.of("clientIp", clientIp));
            log.info("User logged in successfully: {}", req.getUsername());

            return buildLoginResult(sysUser, userDetails, LOGIN_TYPE_PASSWORD, clientIp);
        } catch (Exception e) {
            redisTemplate.opsForValue().increment(limitKey);
            redisTemplate.expire(limitKey, LOCK_TIME_MINUTES, TimeUnit.MINUTES);
            metricsRecorder.increment("flexboot4.auth.login_failed", Map.of(
                    "clientIp", clientIp,
                    "exception", e.getClass().getSimpleName()
            ));
            log.warn("Login failed for user: {} IP: {} Reason: {}", req.getUsername(), clientIp, e.getMessage());
            throw e;
        }
    }

    private LoginResp smsLogin(LoginReq req, String clientIp) {
        LoginMethodOption smsOption = getLoginOptions().method(AuthLoginOptions.METHOD_SMS);
        if (smsOption == null || !smsOption.isEnabled(false)) {
            throw new IllegalStateException("短信验证码登录未启用");
        }

        String phone = normalizePhone(req.getPhone());
        if (!isValidPhone(phone)) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
        verifySmsCode(phone, req.getCode());

        SysUser sysUser = findActiveUserByPhone(phone);
        if (sysUser == null) {
            metricsRecorder.increment("flexboot4.auth.sms_login_failed", Map.of("clientIp", clientIp, "reason", "user_not_found"));
            throw new IllegalStateException(SMS_CODE_INVALID_MSG);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(sysUser.getUsername());
        clearSmsCode(phone);
        metricsRecorder.increment("flexboot4.auth.sms_login_success", Map.of("clientIp", clientIp));
        log.info("User logged in by SMS successfully: {}", sysUser.getUsername());
        return buildLoginResult(sysUser, userDetails, LOGIN_TYPE_SMS, clientIp);
    }

    @Override
    public LoginResp verifyMfa(MfaVerifyReq req, String clientIp) {
        String challengeToken = req.getChallengeToken() == null ? "" : req.getChallengeToken().trim();
        String key = mfaChallengeKey(challengeToken);
        String raw = redisTemplate.opsForValue().get(key);
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("MFA 验证已过期，请重新登录");
        }

        MfaChallenge challenge;
        try {
            challenge = objectMapper.readValue(raw, MfaChallenge.class);
        } catch (Exception e) {
            redisTemplate.delete(key);
            throw new IllegalStateException("MFA 验证已过期，请重新登录");
        }

        if (challenge.attempts() >= MAX_MFA_VERIFY_ATTEMPTS) {
            redisTemplate.delete(key);
            throw new IllegalStateException("MFA 验证失败次数过多，请重新登录");
        }
        if (challenge.clientIp() != null && !challenge.clientIp().isBlank()
                && clientIp != null && !clientIp.isBlank()
                && !challenge.clientIp().equals(clientIp)) {
            redisTemplate.delete(key);
            throw new IllegalStateException("登录环境已变化，请重新登录");
        }

        if (!userMfaService.verifyTotp(challenge.userId(), req.getCode())) {
            MfaChallenge next = new MfaChallenge(
                    challenge.userId(),
                    challenge.username(),
                    challenge.loginType(),
                    challenge.clientIp(),
                    challenge.attempts() + 1
            );
            storeMfaChallenge(key, next);
            throw new IllegalStateException("动态验证码不正确或已过期");
        }

        redisTemplate.delete(key);
        SysUser sysUser = sysUserService.getOne(
                QueryWrapper.create()
                        .where(SysUser::getId).eq(challenge.userId())
                        .and(SysUser::getStatus).eq(1)
                        .and(SysUser::getDelFlag).eq(0)
        );
        if (sysUser == null) {
            throw new SecurityException("Invalid MFA challenge");
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(sysUser.getUsername());
        metricsRecorder.increment("flexboot4.auth.mfa_verify_success", Map.of(
                "clientIp", clientIp,
                "loginType", challenge.loginType()
        ));
        return buildLoginResp(sysUser, userDetails);
    }

    @Override
    public void logout(HttpServletRequest request) {
        String token = jwtUtil.resolveToken(request);
        if (token != null) {
            long remainingTtl = jwtUtil.extractExpiration(token).getTime() - System.currentTimeMillis();
            if (remainingTtl > 0) {
                redisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + token, "revoked", remainingTtl, TimeUnit.MILLISECONDS);
            }

            String username = jwtUtil.extractUsername(token);
            if (username != null) {
                userDetailsCacheService.evictUserCache(username);
            }
        }
        log.info("User logged out");
    }

    @Override
    public String refreshToken(HttpServletRequest request) {
        String token = jwtUtil.resolveToken(request);
        if (token == null) {
            throw new SecurityException("No token provided");
        }

        if (Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + token))) {
            throw new SecurityException("Token is invalid");
        }

        String username = jwtUtil.extractUsername(token);
        if (username == null) {
            throw new SecurityException("Invalid token");
        }

        long remainingTtl = jwtUtil.extractExpiration(token).getTime() - System.currentTimeMillis();
        if (remainingTtl > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + token, "revoked", remainingTtl, TimeUnit.MILLISECONDS);
        }

        SysUser fullUser = sysUserService.getOne(
                QueryWrapper.create().where(SysUser::getUsername).eq(username)
        );
        if (fullUser == null) {
            throw new SecurityException("User not found");
        }

        List<String> roles = fullUser.getRoles() == null
                ? Collections.emptyList()
                : fullUser.getRoles().stream().map(SysRole::getRoleValue).collect(Collectors.toList());
        List<String> permissions = sysMenuService.getPermissionCodes(fullUser.getId());

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(username)
                .password("")
                .authorities(roles.toArray(new String[0]))
                .build();

        return jwtUtil.generateToken(userDetails, fullUser.getId(), roles, permissions);
    }

    @Override
    public String forgetPassword(ForgetPasswordReq req) {
        String email = req.getEmail().toLowerCase().trim();

        SysUser user = sysUserService.getOne(
                QueryWrapper.create().where(SysUser::getEmail).eq(email)
        );

        if (user == null) {
            log.warn("Password reset requested for non-existent email: {}", email);
            return RESET_LINK_SENT_MSG;
        }

        EmailService emailService = emailServiceProvider.getIfAvailable();
        if (emailService == null) {
            log.warn("Password reset email skipped because mail feature is disabled.");
            return RESET_LINK_SENT_MSG;
        }

        String resetToken = UUID.randomUUID().toString().replace("-", "");
        emailService.sendPasswordResetEmail(email, resetToken, user.getId());
        log.info("Password reset email sent for user: {} ({})", user.getUsername(), email);

        return RESET_LINK_SENT_MSG;
    }

    @Override
    public String resetPassword(ResetPasswordReq req) {
        String token = req.getToken().trim();
        String newPassword = req.getNewPassword();

        EmailService emailService = requireEmailService();
        String userId = emailService.validateResetToken(token);
        if (userId == null) {
            log.warn("Invalid or expired reset token");
            throw new SecurityException("Invalid or expired reset link");
        }

        boolean updated = sysUserService.updatePasswordById(userId, newPassword);
        if (!updated) {
            log.error("Failed to update password for user ID: {}", userId);
            throw new RuntimeException("Password reset failed");
        }

        emailService.invalidateResetToken(token);

        SysUser user = sysUserService.getById(userId);
        if (user != null) {
            userDetailsCacheService.evictUserCache(user.getUsername());
        }

        log.info("Password reset successfully for user: {}", user == null ? userId : user.getUsername());
        return "Password reset successfully";
    }

    @Override
    public String adminResetPassword(AdminResetPasswordReq req) {
        EmailService emailService = requireEmailService();
        SysUser user = sysUserService.getById(req.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("User email is required for password reset");
        }

        String resetToken = UUID.randomUUID().toString().replace("-", "");
        emailService.sendPasswordResetEmail(user.getEmail(), resetToken, user.getId());

        log.info("Admin sent password reset link for user ID: {}", req.getUserId());
        return RESET_LINK_SENT_MSG;
    }

    private EmailService requireEmailService() {
        EmailService emailService = emailServiceProvider.getIfAvailable();
        if (emailService == null) {
            throw new IllegalStateException("Mail feature is disabled. Set flexboot4.mail.enabled=true to enable password reset flow.");
        }
        return emailService;
    }

    private LoginResp buildLoginResult(SysUser sysUser, UserDetails userDetails, String loginType, String clientIp) {
        if (userMfaService.isTotpEnabled(sysUser.getId())) {
            return buildMfaRequiredResp(sysUser, loginType, clientIp);
        }
        return buildLoginResp(sysUser, userDetails);
    }

    private LoginResp buildMfaRequiredResp(SysUser sysUser, String loginType, String clientIp) {
        String challengeToken = UUID.randomUUID().toString().replace("-", "");
        MfaChallenge challenge = new MfaChallenge(
                sysUser.getId(),
                sysUser.getUsername(),
                loginType,
                clientIp,
                0
        );
        storeMfaChallenge(mfaChallengeKey(challengeToken), challenge);

        LoginResp loginResp = new LoginResp();
        loginResp.setId(sysUser.getId());
        loginResp.setUsername(sysUser.getUsername());
        loginResp.setRealName(sysUser.getRealName());
        loginResp.setMfaRequired(true);
        loginResp.setMfaChallengeToken(challengeToken);
        loginResp.setMfaMethods(List.of("totp"));
        loginResp.setExpiresIn(TimeUnit.MINUTES.toSeconds(MFA_CHALLENGE_TTL_MINUTES));
        metricsRecorder.increment("flexboot4.auth.mfa_challenge_created", Map.of(
                "clientIp", clientIp,
                "loginType", loginType
        ));
        return loginResp;
    }

    private LoginResp buildLoginResp(SysUser sysUser, UserDetails userDetails) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        List<String> permissions = sysMenuService.getPermissionCodes(sysUser.getId());
        String token = jwtUtil.generateToken(userDetails, sysUser.getId(), roles, permissions);

        LoginResp loginResp = new LoginResp();
        loginResp.setId(sysUser.getId());
        loginResp.setUsername(sysUser.getUsername());
        loginResp.setRealName(sysUser.getRealName());
        loginResp.setRoles(roles);
        loginResp.setAccessToken(token);
        loginResp.setMfaRequired(false);
        return loginResp;
    }

    private void storeMfaChallenge(String key, MfaChallenge challenge) {
        try {
            redisTemplate.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(challenge),
                    MFA_CHALLENGE_TTL_MINUTES,
                    TimeUnit.MINUTES
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to store MFA challenge", e);
        }
    }

    private String mfaChallengeKey(String challengeToken) {
        if (challengeToken == null || challengeToken.isBlank()) {
            throw new IllegalStateException("MFA 验证已过期，请重新登录");
        }
        return MFA_CHALLENGE_KEY_PREFIX + challengeToken;
    }

    private record MfaChallenge(
            String userId,
            String username,
            String loginType,
            String clientIp,
            int attempts
    ) {
    }

    private void verifySmsCode(String phone, String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalStateException(SMS_CODE_INVALID_MSG);
        }

        String failKey = SMS_FAIL_KEY_PREFIX + phone;
        String failValue = redisTemplate.opsForValue().get(failKey);
        int failCount = failValue == null ? 0 : Integer.parseInt(failValue);
        if (failCount >= MAX_SMS_VERIFY_ATTEMPTS) {
            throw new IllegalStateException(SMS_CODE_INVALID_MSG);
        }

        String codeKey = SMS_CODE_KEY_PREFIX + phone;
        String storedHash = redisTemplate.opsForValue().get(codeKey);
        if (storedHash == null || !storedHash.equals(hashSmsCode(phone, code.trim()))) {
            redisTemplate.opsForValue().increment(failKey);
            redisTemplate.expire(failKey, DEFAULT_SMS_TTL_MINUTES, TimeUnit.MINUTES);
            throw new IllegalStateException(SMS_CODE_INVALID_MSG);
        }
    }

    private void clearSmsCode(String phone) {
        redisTemplate.delete(SMS_CODE_KEY_PREFIX + phone);
        redisTemplate.delete(SMS_FAIL_KEY_PREFIX + phone);
    }

    private SysUser findActiveUserByPhone(String phone) {
        List<SysUser> users = sysUserService.list(
                QueryWrapper.create()
                        .where(SysUser::getPhone).eq(phone)
                        .and(SysUser::getStatus).eq(1)
                        .and(SysUser::getDelFlag).eq(0)
        );
        if (users == null || users.isEmpty()) {
            return null;
        }
        if (users.size() > 1) {
            throw new IllegalStateException("手机号绑定了多个用户，请先清理用户数据");
        }
        return users.getFirst();
    }

    private long incrementDailyCounter(String dailyKey) {
        return incrementCounter(dailyKey, MAX_DAILY_TTL_MINUTES);
    }

    private void enforceSmsIpRateLimit(String clientIp) {
        String normalizedIp = normalizeClientIp(clientIp);
        int hourlyLimit = configInt(SMS_IP_HOURLY_LIMIT_KEY, DEFAULT_SMS_IP_HOURLY_LIMIT);
        long hourlyCount = incrementCounter(SMS_IP_HOUR_KEY_PREFIX + normalizedIp, SMS_IP_HOURLY_TTL_MINUTES);
        if (hourlyCount > hourlyLimit) {
            metricsRecorder.increment("flexboot4.auth.sms_send_limited", Map.of(
                    "clientIp", normalizedIp,
                    "reason", "ip_hour"
            ));
            throw new IllegalStateException(SMS_SEND_TOO_FREQUENT_MSG);
        }

        int dailyLimit = configInt(SMS_IP_DAILY_LIMIT_KEY, DEFAULT_SMS_IP_DAILY_LIMIT);
        long dailyCount = incrementCounter(SMS_IP_DAILY_KEY_PREFIX + normalizedIp, MAX_DAILY_TTL_MINUTES);
        if (dailyCount > dailyLimit) {
            metricsRecorder.increment("flexboot4.auth.sms_send_limited", Map.of(
                    "clientIp", normalizedIp,
                    "reason", "ip_daily"
            ));
            throw new IllegalStateException(SMS_SEND_DAILY_LIMIT_MSG);
        }
    }

    private long incrementCounter(String key, long ttlMinutes) {
        Long value = redisTemplate.opsForValue().increment(key);
        if (value != null && value == 1L) {
            redisTemplate.expire(key, ttlMinutes, TimeUnit.MINUTES);
        }
        return value == null ? 0L : value;
    }

    private String normalizeClientIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "unknown";
        }
        return clientIp.trim();
    }

    private String randomNumericCode(int length) {
        int normalizedLength = Math.max(4, Math.min(length, 8));
        StringBuilder code = new StringBuilder(normalizedLength);
        for (int i = 0; i < normalizedLength; i++) {
            code.append(secureRandom.nextInt(10));
        }
        return code.toString();
    }

    private Map<String, String> smsTemplateParams(String code) {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("1", code);
        params.put("2", String.valueOf(DEFAULT_SMS_TTL_MINUTES));
        return params;
    }

    private String hashSmsCode(String phone, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(jwtUtil.getSigningSecretBytes(), "HmacSHA256"));
            byte[] digest = mac.doFinal((phone + ":" + code).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash SMS code", e);
        }
    }

    private static String normalizePhone(String phone) {
        return phone == null ? "" : phone.trim().replaceAll("\\s+", "");
    }

    private static boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }

    private static int optionInt(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private int configInt(String key, int defaultValue) {
        String value = configLookupService.getConfigValue(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed <= 0 ? defaultValue : parsed;
        } catch (NumberFormatException e) {
            log.warn("Invalid integer config value, key={}, value={}", key, value);
            return defaultValue;
        }
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
