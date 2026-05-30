package com.yunlbd.flexboot4.service.sys.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.dto.sys.SecurityEmailBindReq;
import com.yunlbd.flexboot4.dto.sys.SecurityEmailBindResp;
import com.yunlbd.flexboot4.dto.sys.SecurityEmailCodeReq;
import com.yunlbd.flexboot4.entity.sys.SysUser;
import com.yunlbd.flexboot4.metrics.MetricsRecorder;
import com.yunlbd.flexboot4.security.JwtUtil;
import com.yunlbd.flexboot4.security.UserDetailsCacheService;
import com.yunlbd.flexboot4.service.sys.EmailService;
import com.yunlbd.flexboot4.service.sys.SysUserService;
import com.yunlbd.flexboot4.service.sys.UserSecurityEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSecurityEmailServiceImpl implements UserSecurityEmailService {

    private static final String BIND_CODE_KEY_PREFIX = "user:security-email:bind-code:";
    private static final String BIND_COOLDOWN_KEY_PREFIX = "user:security-email:bind-code:cooldown:";
    private static final String BIND_DAILY_KEY_PREFIX = "user:security-email:bind-code:daily:";
    private static final String BIND_FAIL_KEY_PREFIX = "user:security-email:bind-code:fail:";
    private static final int DEFAULT_EMAIL_CODE_LENGTH = 6;
    private static final int DEFAULT_EMAIL_TTL_MINUTES = 5;
    private static final int DEFAULT_EMAIL_COOLDOWN_SECONDS = 60;
    private static final int DEFAULT_EMAIL_DAILY_LIMIT = 10;
    private static final int MAX_EMAIL_VERIFY_ATTEMPTS = 5;
    private static final long MAX_DAILY_TTL_MINUTES = 1440;
    private static final String SEND_SUCCESS_MSG = "验证码已发送，请注意查收";

    private final StringRedisTemplate redisTemplate;
    private final SysUserService sysUserService;
    private final UserDetailsCacheService userDetailsCacheService;
    private final ObjectProvider<EmailService> emailServiceProvider;
    private final JwtUtil jwtUtil;
    private final MetricsRecorder metricsRecorder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String sendBindCode(SysUser currentUser, SecurityEmailCodeReq req) {
        SysUser user = requireCurrentUser(currentUser);
        String email = normalizeEmail(req.getEmail());
        validateBindableEmail(user.getId(), email);

        if (Objects.equals(normalizeEmail(user.getEmail()), email)) {
            throw new IllegalStateException("该邮箱已绑定当前账号");
        }

        String cooldownKey = bindCooldownKey(user.getId(), email);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new IllegalStateException("验证码发送过于频繁，请稍后再试");
        }

        String dailyKey = bindDailyKey(user.getId(), email);
        long dailyCount = incrementDailyCounter(dailyKey);
        if (dailyCount > DEFAULT_EMAIL_DAILY_LIMIT) {
            metricsRecorder.increment("flexboot4.user.security_email.code_send_limited", Map.of("userId", user.getId()));
            throw new IllegalStateException("验证码发送次数已达今日上限");
        }

        EmailService emailService = emailServiceProvider.getIfAvailable();
        if (emailService == null) {
            metricsRecorder.increment("flexboot4.user.security_email.sender_missing", Map.of("userId", user.getId()));
            throw new IllegalStateException("邮件发送服务未启用，请先配置邮件服务");
        }

        String code = randomNumericCode(DEFAULT_EMAIL_CODE_LENGTH);
        String codeKey = bindCodeKey(user.getId(), email);
        String failKey = bindFailKey(user.getId(), email);
        redisTemplate.opsForValue().set(codeKey, hashCode(email, code), DEFAULT_EMAIL_TTL_MINUTES, TimeUnit.MINUTES);
        redisTemplate.delete(failKey);
        redisTemplate.opsForValue().set(cooldownKey, "1", DEFAULT_EMAIL_COOLDOWN_SECONDS, TimeUnit.SECONDS);

        try {
            emailService.sendVerificationCodeEmail(email, code, DEFAULT_EMAIL_TTL_MINUTES);
            metricsRecorder.increment("flexboot4.user.security_email.code_sent", Map.of("userId", user.getId()));
            return SEND_SUCCESS_MSG;
        } catch (Exception e) {
            redisTemplate.delete(codeKey);
            redisTemplate.delete(failKey);
            log.warn("Failed to send security email bind code. userId={}, email={}, reason={}",
                    user.getId(), maskEmail(email), e.getMessage());
            metricsRecorder.increment("flexboot4.user.security_email.send_failed", Map.of(
                    "userId", user.getId(),
                    "exception", e.getClass().getSimpleName()
            ));
            throw new IllegalStateException("验证码发送失败，请稍后再试");
        }
    }

    @Override
    public SecurityEmailBindResp bindEmail(SysUser currentUser, SecurityEmailBindReq req) {
        SysUser user = requireCurrentUser(currentUser);
        String email = normalizeEmail(req.getEmail());
        validateBindableEmail(user.getId(), email);
        if (Objects.equals(normalizeEmail(user.getEmail()), email)) {
            throw new IllegalStateException("该邮箱已绑定当前账号");
        }
        verifyBindCode(user.getId(), email, req.getCode());

        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setEmail(email);
        boolean updated = sysUserService.updateById(update, true);
        if (!updated) {
            throw new IllegalStateException("绑定备用邮箱失败，请稍后再试");
        }

        user.setEmail(email);
        clearBindCode(user.getId(), email);
        userDetailsCacheService.evictUserCache(user.getUsername());
        refreshAuthenticationPrincipal(user.getId(), email);
        metricsRecorder.increment("flexboot4.user.security_email.bound", Map.of("userId", user.getId()));

        return new SecurityEmailBindResp(true, maskEmail(email));
    }

    private void validateBindableEmail(String currentUserId, String email) {
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }

        List<SysUser> users = sysUserService.list(
                QueryWrapper.create()
                        .where("lower(btrim(email)) = ?", email)
                        .and(SysUser::getDelFlag).eq(0)
        );
        if (users == null || users.isEmpty()) {
            return;
        }

        boolean boundByOther = users.stream()
                .anyMatch(user -> !Objects.equals(user.getId(), currentUserId));
        if (boundByOther) {
            throw new IllegalStateException("该邮箱已被其他账号绑定");
        }
    }

    private void verifyBindCode(String userId, String email, String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalStateException("验证码不正确或已过期");
        }

        String failKey = bindFailKey(userId, email);
        String failValue = redisTemplate.opsForValue().get(failKey);
        int failCount = failValue == null ? 0 : Integer.parseInt(failValue);
        if (failCount >= MAX_EMAIL_VERIFY_ATTEMPTS) {
            throw new IllegalStateException("验证码不正确或已过期");
        }

        String storedHash = redisTemplate.opsForValue().get(bindCodeKey(userId, email));
        if (storedHash == null || !storedHash.equals(hashCode(email, code.trim()))) {
            redisTemplate.opsForValue().increment(failKey);
            redisTemplate.expire(failKey, DEFAULT_EMAIL_TTL_MINUTES, TimeUnit.MINUTES);
            throw new IllegalStateException("验证码不正确或已过期");
        }
    }

    private void refreshAuthenticationPrincipal(String userId, String email) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof com.yunlbd.flexboot4.security.LoginUser loginUser
                && loginUser.getSysUser() != null
                && Objects.equals(loginUser.getSysUser().getId(), userId)) {
            loginUser.getSysUser().setEmail(email);
        }
    }

    private long incrementDailyCounter(String dailyKey) {
        Long value = redisTemplate.opsForValue().increment(dailyKey);
        if (value != null && value == 1L) {
            redisTemplate.expire(dailyKey, MAX_DAILY_TTL_MINUTES, TimeUnit.MINUTES);
        }
        return value == null ? 0L : value;
    }

    private void clearBindCode(String userId, String email) {
        redisTemplate.delete(bindCodeKey(userId, email));
        redisTemplate.delete(bindFailKey(userId, email));
    }

    private String randomNumericCode(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(secureRandom.nextInt(10));
        }
        return code.toString();
    }

    private String hashCode(String email, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(jwtUtil.getSigningSecretBytes(), "HmacSHA256"));
            byte[] digest = mac.doFinal((email + ":" + code).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash email code", e);
        }
    }

    private SysUser requireCurrentUser(SysUser user) {
        if (user == null || user.getId() == null || user.getId().isBlank()) {
            throw new SecurityException("未认证或令牌无效/过期");
        }
        return user;
    }

    private String bindCodeKey(String userId, String email) {
        return BIND_CODE_KEY_PREFIX + userId + ":" + email;
    }

    private String bindCooldownKey(String userId, String email) {
        return BIND_COOLDOWN_KEY_PREFIX + userId + ":" + email;
    }

    private String bindDailyKey(String userId, String email) {
        return BIND_DAILY_KEY_PREFIX + userId + ":" + email;
    }

    private String bindFailKey(String userId, String email) {
        return BIND_FAIL_KEY_PREFIX + userId + ":" + email;
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isValidEmail(String email) {
        return email != null && email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

    private static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return email;
        }
        int visibleLength = at > 3 ? 3 : 1;
        return email.substring(0, visibleLength) + "***" + email.substring(at);
    }
}
