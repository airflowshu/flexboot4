package com.yunlbd.flexboot4.service.sys;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.dto.sys.SecurityPhoneBindReq;
import com.yunlbd.flexboot4.dto.sys.SecurityPhoneBindResp;
import com.yunlbd.flexboot4.dto.sys.SecurityPhoneCodeReq;
import com.yunlbd.flexboot4.entity.sys.SysUser;
import com.yunlbd.flexboot4.metrics.MetricsRecorder;
import com.yunlbd.flexboot4.security.JwtUtil;
import com.yunlbd.flexboot4.security.UserDetailsCacheService;
import com.yunlbd.flexboot4.service.sys.impl.UserSecurityPhoneServiceImpl;
import com.yunlbd.flexboot4.sms.SmsMessageRequest;
import com.yunlbd.flexboot4.sms.SmsMessageSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserSecurityPhoneServiceImplTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final SysUserService sysUserService = mock(SysUserService.class);
    private final UserDetailsCacheService userDetailsCacheService = mock(UserDetailsCacheService.class);
    private final SmsMessageSender smsMessageSender = mock(SmsMessageSender.class);
    private final ConfigLookupService configLookupService = mock(ConfigLookupService.class);
    private final JwtUtil jwtUtil = new JwtUtil();
    private final MetricsRecorder metricsRecorder = mock(MetricsRecorder.class);
    private UserSecurityPhoneServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "secret", "thisIsASecretKeyThatIsLongEnoughForHmacSha256SecurityRequirement");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey(any())).thenReturn(false);
        when(valueOperations.increment(any())).thenReturn(1L);
        when(sysUserService.list(any(QueryWrapper.class))).thenReturn(List.of());
        when(configLookupService.getConfigValue("auth.login.options")).thenReturn(
                "{\"methods\":{\"password\":{\"enabled\":true},\"sms\":{\"enabled\":false,\"codeLength\":6,\"cooldownSeconds\":60}}}"
        );
        when(configLookupService.getConfigValue("auth.sms.templateId")).thenReturn("1");
        when(configLookupService.getConfigValue("auth.sms.configId")).thenReturn("");

        service = new UserSecurityPhoneServiceImpl(
                redisTemplate,
                sysUserService,
                userDetailsCacheService,
                objectProvider(smsMessageSender),
                configLookupService,
                jwtUtil,
                metricsRecorder,
                new ObjectMapper()
        );
    }

    @Test
    void sendBindCodeUsesIndependentRedisKeysAndSmsSender() {
        SecurityPhoneCodeReq req = new SecurityPhoneCodeReq();
        req.setPhone("13800138000");

        String message = service.sendBindCode(currentUser(), req);

        assertThat(message).contains("验证码已发送");
        verify(valueOperations).set(
                eq("user:security-phone:bind-code:u1:13800138000"),
                any(String.class),
                eq(5L),
                eq(TimeUnit.MINUTES)
        );
        verify(valueOperations).set(
                "user:security-phone:bind-code:cooldown:u1:13800138000",
                "1",
                60L,
                TimeUnit.SECONDS
        );
        ArgumentCaptor<SmsMessageRequest> captor = ArgumentCaptor.forClass(SmsMessageRequest.class);
        verify(smsMessageSender).send(captor.capture());
        assertThat(captor.getValue().phone()).isEqualTo("13800138000");
        assertThat(captor.getValue().templateParams().get("1")).matches("\\d{6}");
    }

    @Test
    void sendBindCodeRejectsPhoneBoundByOtherUser() {
        SysUser other = new SysUser();
        other.setId("u2");
        other.setPhone("13800138000");
        when(sysUserService.list(any(QueryWrapper.class))).thenReturn(List.of(other));

        SecurityPhoneCodeReq req = new SecurityPhoneCodeReq();
        req.setPhone("13800138000");

        assertThatThrownBy(() -> service.sendBindCode(currentUser(), req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("已被其他账号绑定");
        verify(smsMessageSender, never()).send(any());
    }

    @Test
    void sendBindCodeRejectsMissingSmsSender() {
        service = new UserSecurityPhoneServiceImpl(
                redisTemplate,
                sysUserService,
                userDetailsCacheService,
                objectProvider(null),
                configLookupService,
                jwtUtil,
                metricsRecorder,
                new ObjectMapper()
        );

        SecurityPhoneCodeReq req = new SecurityPhoneCodeReq();
        req.setPhone("13800138000");

        assertThatThrownBy(() -> service.sendBindCode(currentUser(), req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("短信发送服务未启用");
    }

    @Test
    void bindPhoneUpdatesCurrentUserAndClearsCode() {
        SysUser currentUser = currentUser();
        SecurityPhoneCodeReq codeReq = new SecurityPhoneCodeReq();
        codeReq.setPhone("13800138000");
        service.sendBindCode(currentUser, codeReq);
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                eq("user:security-phone:bind-code:u1:13800138000"),
                hashCaptor.capture(),
                eq(5L),
                eq(TimeUnit.MINUTES)
        );
        ArgumentCaptor<SmsMessageRequest> smsCaptor = ArgumentCaptor.forClass(SmsMessageRequest.class);
        verify(smsMessageSender).send(smsCaptor.capture());
        String code = smsCaptor.getValue().templateParams().get("1");

        SecurityPhoneBindReq req = new SecurityPhoneBindReq();
        req.setPhone("13800138000");
        req.setCode(code);
        String codeKey = "user:security-phone:bind-code:u1:13800138000";
        when(valueOperations.get("user:security-phone:bind-code:fail:u1:13800138000")).thenReturn(null);
        when(valueOperations.get(codeKey)).thenReturn(hashCaptor.getValue());
        when(sysUserService.updateById(any(SysUser.class), eq(true))).thenReturn(true);

        SecurityPhoneBindResp resp = service.bindPhone(currentUser, req);

        assertThat(resp.bound()).isTrue();
        assertThat(resp.phoneMasked()).isEqualTo("138****8000");
        assertThat(currentUser.getPhone()).isEqualTo("13800138000");
        ArgumentCaptor<SysUser> updateCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserService).updateById(updateCaptor.capture(), eq(true));
        assertThat(updateCaptor.getValue().getId()).isEqualTo("u1");
        assertThat(updateCaptor.getValue().getPhone()).isEqualTo("13800138000");
        verify(redisTemplate).delete(codeKey);
        verify(redisTemplate, atLeastOnce()).delete("user:security-phone:bind-code:fail:u1:13800138000");
        verify(userDetailsCacheService).evictUserCache("alice");
    }

    @Test
    void bindPhoneRejectsInvalidCode() {
        SecurityPhoneBindReq req = new SecurityPhoneBindReq();
        req.setPhone("13800138000");
        req.setCode("000000");
        when(valueOperations.get("user:security-phone:bind-code:fail:u1:13800138000")).thenReturn(null);
        when(valueOperations.get("user:security-phone:bind-code:u1:13800138000")).thenReturn(null);

        assertThatThrownBy(() -> service.bindPhone(currentUser(), req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("验证码不正确或已过期");
        verify(sysUserService, never()).updateById(any(SysUser.class), eq(true));
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> objectProvider(T instance) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(instance);
        return provider;
    }

    private static SysUser currentUser() {
        SysUser user = new SysUser();
        user.setId("u1");
        user.setUsername("alice");
        user.setStatus(1);
        user.setDelFlag(0);
        return user;
    }

}
