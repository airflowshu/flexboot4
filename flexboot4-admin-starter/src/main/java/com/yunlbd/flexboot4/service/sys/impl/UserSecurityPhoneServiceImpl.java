package com.yunlbd.flexboot4.service.sys.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.dto.AuthLoginOptions;
import com.yunlbd.flexboot4.dto.LoginMethodOption;
import com.yunlbd.flexboot4.dto.sys.SecurityPhoneBindReq;
import com.yunlbd.flexboot4.dto.sys.SecurityPhoneBindResp;
import com.yunlbd.flexboot4.dto.sys.SecurityPhoneCodeReq;
import com.yunlbd.flexboot4.entity.sys.SysUser;
import com.yunlbd.flexboot4.metrics.MetricsRecorder;
import com.yunlbd.flexboot4.security.JwtUtil;
import com.yunlbd.flexboot4.security.UserDetailsCacheService;
import com.yunlbd.flexboot4.service.sys.ConfigLookupService;
import com.yunlbd.flexboot4.service.sys.SysUserService;
import com.yunlbd.flexboot4.service.sys.UserSecurityPhoneService;
import com.yunlbd.flexboot4.sms.SmsMessageRequest;
import com.yunlbd.flexboot4.sms.SmsMessageSender;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSecurityPhoneServiceImpl implements UserSecurityPhoneService {

    private static final String BIND_CODE_KEY_PREFIX = "user:security-phone:bind-code:";
    private static final String BIND_COOLDOWN_KEY_PREFIX = "user:security-phone:bind-code:cooldown:";
    private static final String BIND_DAILY_KEY_PREFIX = "user:security-phone:bind-code:daily:";
    private static final String BIND_FAIL_KEY_PREFIX = "user:security-phone:bind-code:fail:";
    private static final String SMS_CONFIG_ID_KEY = "auth.sms.configId";
    private static final String SMS_TEMPLATE_ID_KEY = "auth.sms.templateId";
    private static final String LOGIN_OPTIONS_KEY = "auth.login.options";
    private static final int DEFAULT_SMS_CODE_LENGTH = 6;
    private static final int DEFAULT_SMS_TTL_MINUTES = 5;
    private static final int DEFAULT_SMS_COOLDOWN_SECONDS = 60;
    private static final int DEFAULT_SMS_DAILY_LIMIT = 10;
    private static final int MAX_SMS_VERIFY_ATTEMPTS = 5;
    private static final long MAX_DAILY_TTL_MINUTES = 1440;
    private static final String SEND_SUCCESS_MSG = "验证码已发送，请注意查收";

    private final StringRedisTemplate redisTemplate;
    private final SysUserService sysUserService;
    private final UserDetailsCacheService userDetailsCacheService;
    private final ObjectProvider<SmsMessageSender> smsMessageSenderProvider;
    private final ConfigLookupService configLookupService;
    private final JwtUtil jwtUtil;
    private final MetricsRecorder metricsRecorder;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String sendBindCode(SysUser currentUser, SecurityPhoneCodeReq req) {
        SysUser user = requireCurrentUser(currentUser);
        String phone = normalizePhone(req.getPhone());
        validateBindablePhone(user.getId(), phone);

        if (Objects.equals(normalizePhone(user.getPhone()), phone)) {
            throw new IllegalStateException("该手机号已绑定当前账号");
        }

        LoginMethodOption smsOption = getSmsOption();
        int codeLength = optionInt(smsOption.getCodeLength(), DEFAULT_SMS_CODE_LENGTH);
        int cooldownSeconds = optionInt(smsOption.getCooldownSeconds(), DEFAULT_SMS_COOLDOWN_SECONDS);

        String cooldownKey = bindCooldownKey(user.getId(), phone);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new IllegalStateException("验证码发送过于频繁，请稍后再试");
        }

        String dailyKey = bindDailyKey(user.getId(), phone);
        long dailyCount = incrementDailyCounter(dailyKey);
        if (dailyCount > DEFAULT_SMS_DAILY_LIMIT) {
            metricsRecorder.increment("flexboot4.user.security_phone.sms_send_limited", Map.of("userId", user.getId()));
            throw new IllegalStateException("验证码发送次数已达今日上限");
        }

        SmsMessageSender sender = smsMessageSenderProvider.getIfAvailable();
        if (sender == null) {
            metricsRecorder.increment("flexboot4.user.security_phone.sms_sender_missing", Map.of("userId", user.getId()));
            throw new IllegalStateException("短信发送服务未启用，请先配置短信供应商");
        }

        String code = randomNumericCode(codeLength);
        String codeKey = bindCodeKey(user.getId(), phone);
        String failKey = bindFailKey(user.getId(), phone);
        redisTemplate.opsForValue().set(codeKey, hashSmsCode(phone, code), DEFAULT_SMS_TTL_MINUTES, TimeUnit.MINUTES);
        redisTemplate.delete(failKey);
        redisTemplate.opsForValue().set(cooldownKey, "1", cooldownSeconds, TimeUnit.SECONDS);

        try {
            sender.send(new SmsMessageRequest(
                    phone,
                    configLookupService.getConfigValue(SMS_TEMPLATE_ID_KEY),
                    smsTemplateParams(code),
                    configLookupService.getConfigValue(SMS_CONFIG_ID_KEY)
            ));
            metricsRecorder.increment("flexboot4.user.security_phone.sms_code_sent", Map.of("userId", user.getId()));
            return SEND_SUCCESS_MSG;
        } catch (Exception e) {
            redisTemplate.delete(codeKey);
            redisTemplate.delete(failKey);
            log.warn("Failed to send security phone bind code. userId={}, phone={}, reason={}",
                    user.getId(), maskPhone(phone), e.getMessage());
            metricsRecorder.increment("flexboot4.user.security_phone.sms_send_failed", Map.of(
                    "userId", user.getId(),
                    "exception", e.getClass().getSimpleName()
            ));
            throw new IllegalStateException("验证码发送失败，请稍后再试");
        }
    }

    @Override
    public SecurityPhoneBindResp bindPhone(SysUser currentUser, SecurityPhoneBindReq req) {
        SysUser user = requireCurrentUser(currentUser);
        String phone = normalizePhone(req.getPhone());
        validateBindablePhone(user.getId(), phone);
        if (Objects.equals(normalizePhone(user.getPhone()), phone)) {
            throw new IllegalStateException("该手机号已绑定当前账号");
        }
        verifyBindCode(user.getId(), phone, req.getCode());

        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPhone(phone);
        boolean updated = sysUserService.updateById(update, true);
        if (!updated) {
            throw new IllegalStateException("绑定密保手机失败，请稍后再试");
        }

        user.setPhone(phone);
        clearBindCode(user.getId(), phone);
        userDetailsCacheService.evictUserCache(user.getUsername());
        refreshAuthenticationPrincipal(user.getId(), phone);
        metricsRecorder.increment("flexboot4.user.security_phone.bound", Map.of("userId", user.getId()));

        return new SecurityPhoneBindResp(true, maskPhone(phone));
    }

    private void validateBindablePhone(String currentUserId, String phone) {
        if (!isValidPhone(phone)) {
            throw new IllegalArgumentException("手机号格式不正确");
        }

        List<SysUser> users = sysUserService.list(
                QueryWrapper.create()
                        .where(SysUser::getPhone).eq(phone)
                        .and(SysUser::getDelFlag).eq(0)
        );
        if (users == null || users.isEmpty()) {
            return;
        }

        boolean boundByOther = users.stream()
                .anyMatch(user -> !Objects.equals(user.getId(), currentUserId));
        if (boundByOther) {
            throw new IllegalStateException("该手机号已被其他账号绑定");
        }
    }

    private void verifyBindCode(String userId, String phone, String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalStateException("验证码不正确或已过期");
        }

        String failKey = bindFailKey(userId, phone);
        String failValue = redisTemplate.opsForValue().get(failKey);
        int failCount = failValue == null ? 0 : Integer.parseInt(failValue);
        if (failCount >= MAX_SMS_VERIFY_ATTEMPTS) {
            throw new IllegalStateException("验证码不正确或已过期");
        }

        String storedHash = redisTemplate.opsForValue().get(bindCodeKey(userId, phone));
        if (storedHash == null || !storedHash.equals(hashSmsCode(phone, code.trim()))) {
            redisTemplate.opsForValue().increment(failKey);
            redisTemplate.expire(failKey, DEFAULT_SMS_TTL_MINUTES, TimeUnit.MINUTES);
            throw new IllegalStateException("验证码不正确或已过期");
        }
    }

    private void refreshAuthenticationPrincipal(String userId, String phone) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof com.yunlbd.flexboot4.security.LoginUser loginUser
                && loginUser.getSysUser() != null
                && Objects.equals(loginUser.getSysUser().getId(), userId)) {
            loginUser.getSysUser().setPhone(phone);
        }
    }

    private LoginMethodOption getSmsOption() {
        String raw = configLookupService.getConfigValue(LOGIN_OPTIONS_KEY);
        if (raw == null || raw.isBlank()) {
            return AuthLoginOptions.defaults().method(AuthLoginOptions.METHOD_SMS);
        }
        try {
            return objectMapper.readValue(raw, AuthLoginOptions.class)
                    .method(AuthLoginOptions.METHOD_SMS);
        } catch (Exception e) {
            log.warn("Invalid auth login options config when binding security phone, fallback to defaults: {}", e.getMessage());
            return AuthLoginOptions.defaults().method(AuthLoginOptions.METHOD_SMS);
        }
    }

    private long incrementDailyCounter(String dailyKey) {
        Long value = redisTemplate.opsForValue().increment(dailyKey);
        if (value != null && value == 1L) {
            redisTemplate.expire(dailyKey, MAX_DAILY_TTL_MINUTES, TimeUnit.MINUTES);
        }
        return value == null ? 0L : value;
    }

    private void clearBindCode(String userId, String phone) {
        redisTemplate.delete(bindCodeKey(userId, phone));
        redisTemplate.delete(bindFailKey(userId, phone));
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

    private SysUser requireCurrentUser(SysUser user) {
        if (user == null || user.getId() == null || user.getId().isBlank()) {
            throw new SecurityException("未认证或令牌无效/过期");
        }
        return user;
    }

    private String bindCodeKey(String userId, String phone) {
        return BIND_CODE_KEY_PREFIX + userId + ":" + phone;
    }

    private String bindCooldownKey(String userId, String phone) {
        return BIND_COOLDOWN_KEY_PREFIX + userId + ":" + phone;
    }

    private String bindDailyKey(String userId, String phone) {
        return BIND_DAILY_KEY_PREFIX + userId + ":" + phone;
    }

    private String bindFailKey(String userId, String phone) {
        return BIND_FAIL_KEY_PREFIX + userId + ":" + phone;
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

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
