package com.yunlbd.flexboot4.service.sys;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.dto.AuthLoginOptions;
import com.yunlbd.flexboot4.dto.LoginReq;
import com.yunlbd.flexboot4.dto.LoginResp;
import com.yunlbd.flexboot4.dto.SmsCodeReq;
import com.yunlbd.flexboot4.entity.sys.SysUser;
import com.yunlbd.flexboot4.metrics.MetricsRecorder;
import com.yunlbd.flexboot4.security.JwtUtil;
import com.yunlbd.flexboot4.security.UserDetailsCacheService;
import com.yunlbd.flexboot4.service.sys.impl.AuthServiceImpl;
import com.yunlbd.flexboot4.sms.SmsMessageRequest;
import com.yunlbd.flexboot4.sms.SmsMessageSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final UserDetailsService userDetailsService = mock(UserDetailsService.class);
    private final JwtUtil jwtUtil = new JwtUtil();
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final SysMenuService sysMenuService = mock(SysMenuService.class);
    private final UserDetailsCacheService userDetailsCacheService = mock(UserDetailsCacheService.class);
    private final ConfigLookupService configLookupService = mock(ConfigLookupService.class);
    private final SysUserService sysUserService = mock(SysUserService.class);
    private final UserMfaService userMfaService = mock(UserMfaService.class);
    private final MetricsRecorder metricsRecorder = mock(MetricsRecorder.class);
    private final SmsMessageSender smsMessageSender = mock(SmsMessageSender.class);
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "secret", "thisIsASecretKeyThatIsLongEnoughForHmacSha256SecurityRequirement");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 1800000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey(any())).thenReturn(false);
        when(valueOperations.increment(any())).thenReturn(1L);
        when(configLookupService.getConfigValue("auth.login.options")).thenReturn(
                "{\"methods\":{\"password\":{\"enabled\":true},\"sms\":{\"enabled\":true,\"codeLength\":6,\"cooldownSeconds\":60}}}"
        );
        when(configLookupService.getConfigValue("auth.sms.templateId")).thenReturn("1");
        when(configLookupService.getConfigValue("auth.sms.configId")).thenReturn("");
        when(configLookupService.getConfigValue("auth.sms.ipHourlyLimit")).thenReturn(null);
        when(configLookupService.getConfigValue("auth.sms.ipDailyLimit")).thenReturn(null);

        authService = new AuthServiceImpl(
                authenticationManager,
                userDetailsService,
                jwtUtil,
                redisTemplate,
                sysMenuService,
                userDetailsCacheService,
                objectProvider(null),
                objectProvider(smsMessageSender),
                configLookupService,
                sysUserService,
                userMfaService,
                metricsRecorder,
                new ObjectMapper()
        );
    }

    @Test
    void defaultLoginOptionsAreUsedWhenConfigIsMissing() {
        when(configLookupService.getConfigValue("auth.login.options")).thenReturn(null);

        AuthLoginOptions options = authService.getLoginOptions();

        assertThat(options.getMethods().get("password").getEnabled()).isTrue();
        assertThat(options.getMethods().get("sms").getEnabled()).isFalse();
        assertThat(options.getMethods().get("sms").getCodeLength()).isEqualTo(6);
        assertThat(options.getMethods().get("thirdParty").getProviders())
                .extracting("code")
                .containsExactly("github", "qq");
        assertThat(options.getMethods().get("thirdParty").getProviders())
                .extracting("enabled")
                .containsExactly(false, false);
    }

    @Test
    void loginOptionsSupportSimpleProviderConfig() {
        when(configLookupService.getConfigValue("auth.login.options")).thenReturn(
                "{\"methods\":{\"thirdParty\":{\"providers\":[{\"code\":\"github\",\"enabled\":true}]}}}"
        );

        AuthLoginOptions options = authService.getLoginOptions();

        assertThat(options.getMethods().get("thirdParty").getEnabled()).isNull();
        assertThat(options.getMethods().get("thirdParty").getProviders())
                .extracting("code")
                .containsExactly("github");
        assertThat(options.getMethods().get("thirdParty").getProviders().getFirst().getEnabled()).isTrue();
    }

    @Test
    void sendSmsCodeUsesSenderWithCloopenTemplateParams() {
        when(sysUserService.list(any(QueryWrapper.class))).thenReturn(List.of(activeUser()));

        SmsCodeReq req = new SmsCodeReq();
        req.setPhone("13800138000");
        authService.sendSmsCode(req, "127.0.0.1");

        ArgumentCaptor<SmsMessageRequest> captor = ArgumentCaptor.forClass(SmsMessageRequest.class);
        verify(smsMessageSender).send(captor.capture());
        SmsMessageRequest request = captor.getValue();
        assertThat(request.phone()).isEqualTo("13800138000");
        assertThat(request.templateId()).isEqualTo("1");
        assertThat(request.templateParams()).containsEntry("2", "5");
        assertThat(request.templateParams().get("1")).matches("\\d{6}");
        verify(valueOperations).increment("auth:sms-code:ip:hour:127.0.0.1");
        verify(redisTemplate).expire("auth:sms-code:ip:hour:127.0.0.1", 60L, TimeUnit.MINUTES);
        verify(valueOperations).increment("auth:sms-code:ip:daily:127.0.0.1");
        verify(redisTemplate).expire("auth:sms-code:ip:daily:127.0.0.1", 1440L, TimeUnit.MINUTES);
    }

    @Test
    void sendSmsCodeDoesNotSendWhenPhoneIsNotBound() {
        when(sysUserService.list(any(QueryWrapper.class))).thenReturn(List.of());

        SmsCodeReq req = new SmsCodeReq();
        req.setPhone("13800138000");
        String message = authService.sendSmsCode(req, "127.0.0.1");

        assertThat(message).contains("验证码已发送");
        verify(smsMessageSender, never()).send(any());
        verify(valueOperations).increment("auth:sms-code:ip:hour:127.0.0.1");
        verify(valueOperations).increment("auth:sms-code:ip:daily:127.0.0.1");
        verify(valueOperations).increment("auth:sms-code:daily:13800138000");
        verify(valueOperations).set("auth:sms-code:cooldown:13800138000", "1", 60L, TimeUnit.SECONDS);
    }

    @Test
    void sendSmsCodeStopsBeforeUserLookupWhenIpHourlyLimitExceeded() {
        when(valueOperations.increment("auth:sms-code:ip:hour:127.0.0.1")).thenReturn(31L);

        SmsCodeReq req = new SmsCodeReq();
        req.setPhone("13800138000");

        assertThatThrownBy(() -> authService.sendSmsCode(req, "127.0.0.1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("验证码发送过于频繁，请稍后再试");

        verify(sysUserService, never()).list(any(QueryWrapper.class));
        verify(smsMessageSender, never()).send(any());
        verify(valueOperations, never()).increment("auth:sms-code:ip:daily:127.0.0.1");
        verify(valueOperations, never()).increment("auth:sms-code:daily:13800138000");
    }

    @Test
    void sendSmsCodeStopsBeforeUserLookupWhenIpDailyLimitExceeded() {
        when(valueOperations.increment("auth:sms-code:ip:daily:127.0.0.1")).thenReturn(101L);

        SmsCodeReq req = new SmsCodeReq();
        req.setPhone("13800138000");

        assertThatThrownBy(() -> authService.sendSmsCode(req, "127.0.0.1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("验证码发送次数已达今日上限");

        verify(sysUserService, never()).list(any(QueryWrapper.class));
        verify(smsMessageSender, never()).send(any());
        verify(valueOperations).increment("auth:sms-code:ip:hour:127.0.0.1");
        verify(valueOperations).increment("auth:sms-code:ip:daily:127.0.0.1");
        verify(valueOperations, never()).increment("auth:sms-code:daily:13800138000");
    }

    @Test
    void smsLoginWithInvalidCodeThrowsBusinessExceptionAndKeepsStoredCode() {
        when(valueOperations.get("auth:sms-code:fail:13800138000")).thenReturn(null);
        when(valueOperations.get("auth:sms-code:13800138000")).thenReturn("stored-hash");

        assertThatThrownBy(() -> authService.login(smsLoginReq("13800138000", "000000"), "127.0.0.1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("验证码不正确或已过期");

        verify(valueOperations).increment("auth:sms-code:fail:13800138000");
        verify(redisTemplate).expire("auth:sms-code:fail:13800138000", 5L, TimeUnit.MINUTES);
        verify(redisTemplate, never()).delete("auth:sms-code:13800138000");
    }

    @Test
    void smsLoginAcceptsCorrectCodeAfterPreviousInvalidAttempt() {
        when(sysUserService.list(any(QueryWrapper.class))).thenReturn(List.of(activeUser()));

        SmsCodeReq smsCodeReq = new SmsCodeReq();
        smsCodeReq.setPhone("13800138000");
        authService.sendSmsCode(smsCodeReq, "127.0.0.1");

        ArgumentCaptor<SmsMessageRequest> messageCaptor = ArgumentCaptor.forClass(SmsMessageRequest.class);
        verify(smsMessageSender).send(messageCaptor.capture());
        String smsCode = messageCaptor.getValue().templateParams().get("1");

        ArgumentCaptor<String> storedHashCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                eq("auth:sms-code:13800138000"),
                storedHashCaptor.capture(),
                eq(5L),
                eq(TimeUnit.MINUTES)
        );
        String storedHash = storedHashCaptor.getValue();

        clearInvocations(redisTemplate, valueOperations, smsMessageSender, metricsRecorder, sysUserService, userDetailsService);
        when(valueOperations.get("auth:sms-code:fail:13800138000")).thenReturn(null);
        when(valueOperations.get("auth:sms-code:13800138000")).thenReturn("not-matching");

        assertThatThrownBy(() -> authService.login(smsLoginReq("13800138000", "000000"), "127.0.0.1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("验证码不正确或已过期");
        verify(redisTemplate, never()).delete("auth:sms-code:13800138000");

        clearInvocations(redisTemplate, valueOperations, metricsRecorder, sysUserService, userDetailsService, sysMenuService, userMfaService);
        when(valueOperations.get("auth:sms-code:fail:13800138000")).thenReturn("1");
        when(valueOperations.get("auth:sms-code:13800138000")).thenReturn(storedHash);
        when(sysUserService.list(any(QueryWrapper.class))).thenReturn(List.of(activeUser()));
        UserDetails userDetails = User.withUsername("alice").password("x").authorities("ADMIN").build();
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);
        when(sysMenuService.getPermissionCodes("u1")).thenReturn(List.of("sys:user:list"));
        when(userMfaService.isTotpEnabled("u1")).thenReturn(false);

        LoginResp loginResp = authService.login(smsLoginReq("13800138000", smsCode), "127.0.0.1");

        assertThat(loginResp.getAccessToken()).isNotBlank();
        assertThat(loginResp.getMfaRequired()).isFalse();
        verify(redisTemplate).delete("auth:sms-code:13800138000");
        verify(redisTemplate).delete("auth:sms-code:fail:13800138000");
    }

    @Test
    void smsLoginWithExceededVerifyAttemptsThrowsBusinessException() {
        when(valueOperations.get("auth:sms-code:fail:13800138000")).thenReturn("5");

        assertThatThrownBy(() -> authService.login(smsLoginReq("13800138000", "123456"), "127.0.0.1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("验证码不正确或已过期");

        verify(valueOperations, never()).increment("auth:sms-code:fail:13800138000");
        verify(valueOperations, never()).get("auth:sms-code:13800138000");
        verify(redisTemplate, never()).delete("auth:sms-code:13800138000");
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> objectProvider(T instance) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(instance);
        return provider;
    }

    private static LoginReq smsLoginReq(String phone, String code) {
        LoginReq req = new LoginReq();
        req.setLoginType("sms");
        req.setPhone(phone);
        req.setCode(code);
        return req;
    }

    private static SysUser activeUser() {
        SysUser user = new SysUser();
        user.setId("u1");
        user.setUsername("alice");
        user.setPhone("13800138000");
        user.setStatus(1);
        user.setDelFlag(0);
        return user;
    }
}
