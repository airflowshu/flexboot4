package com.yunlbd.flexboot4.service.sys;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.dto.AuthLoginOptions;
import com.yunlbd.flexboot4.dto.SmsCodeReq;
import com.yunlbd.flexboot4.entity.sys.SysUser;
import com.yunlbd.flexboot4.mapper.SysUserMapper;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
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
        ReflectionTestUtils.setField(jwtUtil, "expiration", 7200000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey(any())).thenReturn(false);
        when(valueOperations.increment(any())).thenReturn(1L);
        when(configLookupService.getConfigValue("auth.login.options")).thenReturn(
                "{\"methods\":{\"password\":{\"enabled\":true},\"sms\":{\"enabled\":true,\"codeLength\":6,\"cooldownSeconds\":60}}}"
        );
        when(configLookupService.getConfigValue("auth.sms.templateId")).thenReturn("1");
        when(configLookupService.getConfigValue("auth.sms.configId")).thenReturn("");

        authService = new AuthServiceImpl(
                authenticationManager,
                userDetailsService,
                jwtUtil,
                redisTemplate,
                sysUserMapper,
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
    }

    @Test
    void sendSmsCodeUsesSenderWithCloopenTemplateParams() {
        when(sysUserMapper.selectListByQuery(any(QueryWrapper.class))).thenReturn(List.of(activeUser()));

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
    }

    @Test
    void sendSmsCodeDoesNotSendWhenPhoneIsNotBound() {
        when(sysUserMapper.selectListByQuery(any(QueryWrapper.class))).thenReturn(List.of());

        SmsCodeReq req = new SmsCodeReq();
        req.setPhone("13800138000");
        String message = authService.sendSmsCode(req, "127.0.0.1");

        assertThat(message).contains("验证码已发送");
        verify(smsMessageSender, never()).send(any());
        verify(valueOperations).set("auth:sms-code:cooldown:13800138000", "1", 60L, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> objectProvider(T instance) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(instance);
        return provider;
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
