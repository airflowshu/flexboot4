package com.yunlbd.flexboot4.service.sys.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.dto.sys.UserMfaTotpConfirmReq;
import com.yunlbd.flexboot4.dto.sys.UserMfaTotpDisableReq;
import com.yunlbd.flexboot4.dto.sys.UserMfaTotpSetupResp;
import com.yunlbd.flexboot4.dto.sys.UserMfaTotpStatusResp;
import com.yunlbd.flexboot4.entity.sys.SysUser;
import com.yunlbd.flexboot4.entity.sys.SysUserMfa;
import com.yunlbd.flexboot4.mapper.SysUserMfaMapper;
import com.yunlbd.flexboot4.metrics.MetricsRecorder;
import com.yunlbd.flexboot4.security.MfaSecretCipher;
import com.yunlbd.flexboot4.security.TotpUtil;
import com.yunlbd.flexboot4.security.UserDetailsCacheService;
import com.yunlbd.flexboot4.service.sys.UserMfaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserMfaServiceImpl implements UserMfaService {

    private static final String TYPE_TOTP = "TOTP";
    private static final String DEFAULT_DEVICE_NAME = "认证器应用";
    private static final char[] ACCOUNT_SUFFIX_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final SysUserMfaMapper sysUserMfaMapper;
    private final TotpUtil totpUtil;
    private final MfaSecretCipher mfaSecretCipher;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsCacheService userDetailsCacheService;
    private final MetricsRecorder metricsRecorder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public UserMfaTotpSetupResp setupTotp(SysUser currentUser) {
        SysUser user = requireCurrentUser(currentUser);
        if (isTotpEnabled(user.getId())) {
            throw new IllegalStateException("当前账号已绑定 MFA 设备");
        }

        String secret = totpUtil.generateSecret();
        SysUserMfa mfa = findLatestTotp(user.getId());
        boolean isNew = mfa == null;
        if (mfa == null) {
            mfa = new SysUserMfa();
            mfa.setId(UUID.randomUUID().toString().replace("-", ""));
            mfa.setUserId(user.getId());
            mfa.setType(TYPE_TOTP);
            mfa.setEnabled(false);
            mfa.setDelFlag(0);
            mfa.setVersion(0L);
        }
        mfa.setSecretCiphertext(mfaSecretCipher.encrypt(secret));
        mfa.setDeviceName(DEFAULT_DEVICE_NAME);
        mfa.setEnabled(false);
        String accountName = buildUniqueAccountName(user.getUsername());

        if (isNew) {
            sysUserMfaMapper.insert(mfa);
        } else {
            sysUserMfaMapper.update(mfa, true);
        }

        metricsRecorder.increment("flexboot4.user.mfa_totp.setup", Map.of("userId", user.getId()));
        return new UserMfaTotpSetupResp(
                totpUtil.buildOtpAuthUri(secret, accountName),
                secret,
                TotpUtil.ISSUER,
                accountName,
                TotpUtil.DIGITS,
                TotpUtil.PERIOD_SECONDS
        );
    }

    @Override
    @Transactional
    public UserMfaTotpStatusResp confirmTotp(SysUser currentUser, UserMfaTotpConfirmReq req) {
        SysUser user = requireCurrentUser(currentUser);
        SysUserMfa mfa = findLatestTotp(user.getId());
        if (mfa == null || mfa.getSecretCiphertext() == null || mfa.getSecretCiphertext().isBlank()) {
            throw new IllegalStateException("请先初始化 MFA 绑定");
        }
        String secret = mfaSecretCipher.decrypt(mfa.getSecretCiphertext());
        if (!totpUtil.verify(secret, req.getCode())) {
            throw new IllegalStateException("动态验证码不正确或已过期");
        }

        LocalDateTime now = LocalDateTime.now();
        mfa.setEnabled(true);
        mfa.setConfirmedAt(now);
        mfa.setLastUsedAt(now);
        mfa.setDeviceName(normalizeDeviceName(req.getDeviceName()));
        sysUserMfaMapper.update(mfa, true);

        userDetailsCacheService.evictUserCache(user.getUsername());
        metricsRecorder.increment("flexboot4.user.mfa_totp.enabled", Map.of("userId", user.getId()));
        return toStatus(mfa);
    }

    @Override
    @Transactional
    public UserMfaTotpStatusResp disableTotp(SysUser currentUser, UserMfaTotpDisableReq req) {
        SysUser user = requireCurrentUser(currentUser);
        SysUserMfa mfa = findEnabledTotp(user.getId());
        if (mfa == null) {
            throw new IllegalStateException("当前账号未绑定 MFA 设备");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new SecurityException("当前密码不正确");
        }
        String secret = mfaSecretCipher.decrypt(mfa.getSecretCiphertext());
        if (!totpUtil.verify(secret, req.getCode())) {
            throw new IllegalStateException("动态验证码不正确或已过期");
        }

        mfa.setEnabled(false);
        mfa.setLastUsedAt(LocalDateTime.now());
        sysUserMfaMapper.update(mfa, true);

        userDetailsCacheService.evictUserCache(user.getUsername());
        metricsRecorder.increment("flexboot4.user.mfa_totp.disabled", Map.of("userId", user.getId()));
        return new UserMfaTotpStatusResp(false, null, null);
    }

    @Override
    public UserMfaTotpStatusResp getTotpStatus(String userId) {
        SysUserMfa mfa = findEnabledTotp(userId);
        return toStatus(mfa);
    }

    @Override
    public boolean isTotpEnabled(String userId) {
        return findEnabledTotp(userId) != null;
    }

    @Override
    @Transactional
    public boolean verifyTotp(String userId, String code) {
        SysUserMfa mfa = findEnabledTotp(userId);
        if (mfa == null || mfa.getSecretCiphertext() == null || mfa.getSecretCiphertext().isBlank()) {
            return false;
        }
        String secret = mfaSecretCipher.decrypt(mfa.getSecretCiphertext());
        boolean verified = totpUtil.verify(secret, code);
        if (verified) {
            mfa.setLastUsedAt(LocalDateTime.now());
            sysUserMfaMapper.update(mfa, true);
        }
        return verified;
    }

    private SysUserMfa findEnabledTotp(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        return sysUserMfaMapper.selectOneByQuery(
                QueryWrapper.create()
                        .where("user_id = ?", userId)
                        .and("type = ?", TYPE_TOTP)
                        .and("enabled = ?", true)
                        .and("del_flag = ?", 0)
                        .limit(1)
        );
    }

    private SysUserMfa findLatestTotp(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        return sysUserMfaMapper.selectOneByQuery(
                QueryWrapper.create()
                        .where("user_id = ?", userId)
                        .and("type = ?", TYPE_TOTP)
                        .and("del_flag = ?", 0)
                        .orderBy("create_time DESC")
                        .limit(1)
        );
    }

    private UserMfaTotpStatusResp toStatus(SysUserMfa mfa) {
        if (mfa == null || !Boolean.TRUE.equals(mfa.getEnabled())) {
            return new UserMfaTotpStatusResp(false, null, null);
        }
        return new UserMfaTotpStatusResp(true, TYPE_TOTP, mfa.getDeviceName());
    }

    private String normalizeDeviceName(String deviceName) {
        if (deviceName == null || deviceName.isBlank()) {
            return DEFAULT_DEVICE_NAME;
        }
        String normalized = deviceName.trim();
        return normalized.length() > 120 ? normalized.substring(0, 120) : normalized;
    }

    private String buildUniqueAccountName(String username) {
        String normalizedUsername = username == null || username.isBlank()
                ? "user"
                : username.trim();
        StringBuilder suffix = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            suffix.append(ACCOUNT_SUFFIX_CHARS[secureRandom.nextInt(ACCOUNT_SUFFIX_CHARS.length)]);
        }
        return normalizedUsername + "@" + suffix.toString().toUpperCase(Locale.ROOT);
    }

    private SysUser requireCurrentUser(SysUser user) {
        if (user == null || user.getId() == null || user.getId().isBlank()) {
            throw new SecurityException("未认证或令牌无效/过期");
        }
        return user;
    }

}
