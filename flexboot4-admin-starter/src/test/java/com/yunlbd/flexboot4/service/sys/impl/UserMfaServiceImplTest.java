package com.yunlbd.flexboot4.service.sys.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.dto.sys.UserMfaTotpConfirmReq;
import com.yunlbd.flexboot4.dto.sys.UserMfaTotpSetupResp;
import com.yunlbd.flexboot4.entity.sys.SysUser;
import com.yunlbd.flexboot4.entity.sys.SysUserMfa;
import com.yunlbd.flexboot4.metrics.MetricsRecorder;
import com.yunlbd.flexboot4.security.MfaSecretCipher;
import com.yunlbd.flexboot4.security.TotpUtil;
import com.yunlbd.flexboot4.security.UserDetailsCacheService;
import com.yunlbd.flexboot4.service.sys.SysUserMfaService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserMfaServiceImplTest {

    private final SysUserMfaService sysUserMfaService = mock(SysUserMfaService.class);
    private final TotpUtil totpUtil = mock(TotpUtil.class);
    private final MfaSecretCipher cipher = mock(MfaSecretCipher.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserDetailsCacheService userDetailsCacheService = mock(UserDetailsCacheService.class);
    private final MetricsRecorder metricsRecorder = mock(MetricsRecorder.class);
    private final UserMfaServiceImpl service = new UserMfaServiceImpl(
            sysUserMfaService,
            totpUtil,
            cipher,
            passwordEncoder,
            userDetailsCacheService,
            metricsRecorder
    );

    @Test
    void setupTotpCreatesPendingSecretWithoutEnablingMfa() {
        when(totpUtil.generateSecret()).thenReturn("ABCDEF234567");
        when(totpUtil.buildOtpAuthUri(eq("ABCDEF234567"), argThat(account -> account.matches("alice@[A-Z2-9]{6}"))))
                .thenReturn("otpauth://totp/FlexBoot4:alice@A7K3P2");
        when(cipher.encrypt("ABCDEF234567")).thenReturn("encrypted");

        UserMfaTotpSetupResp resp = service.setupTotp(currentUser());

        assertThat(resp.manualKey()).isEqualTo("ABCDEF234567");
        assertThat(resp.accountName()).matches("alice@[A-Z2-9]{6}");
        verify(sysUserMfaService).save(any(SysUserMfa.class));
    }

    @Test
    void confirmTotpEnablesMfaWhenCodeIsValid() {
        SysUserMfa pending = pendingMfa();
        when(sysUserMfaService.getOne(any(QueryWrapper.class))).thenReturn(pending);
        when(cipher.decrypt("encrypted")).thenReturn("ABCDEF234567");
        when(totpUtil.verify("ABCDEF234567", "123456")).thenReturn(true);
        UserMfaTotpConfirmReq req = new UserMfaTotpConfirmReq();
        req.setCode("123456");
        req.setDeviceName("Microsoft Authenticator");

        var resp = service.confirmTotp(currentUser(), req);

        assertThat(resp.enabled()).isTrue();
        assertThat(resp.deviceName()).isEqualTo("Microsoft Authenticator");
        assertThat(pending.getEnabled()).isTrue();
        verify(userDetailsCacheService).evictUserCache("alice");
    }

    private static SysUser currentUser() {
        SysUser user = new SysUser();
        user.setId("u1");
        user.setUsername("alice");
        user.setPassword("{noop}11111111");
        return user;
    }

    private static SysUserMfa pendingMfa() {
        SysUserMfa mfa = new SysUserMfa();
        mfa.setId("mfa1");
        mfa.setUserId("u1");
        mfa.setType("TOTP");
        mfa.setSecretCiphertext("encrypted");
        mfa.setEnabled(false);
        mfa.setDelFlag(0);
        return mfa;
    }
}
