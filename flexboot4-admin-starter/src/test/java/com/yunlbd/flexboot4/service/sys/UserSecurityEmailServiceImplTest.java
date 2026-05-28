package com.yunlbd.flexboot4.service.sys;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.dto.sys.SecurityEmailBindReq;
import com.yunlbd.flexboot4.dto.sys.SecurityEmailBindResp;
import com.yunlbd.flexboot4.dto.sys.SecurityEmailCodeReq;
import com.yunlbd.flexboot4.entity.sys.SysUser;
import com.yunlbd.flexboot4.mapper.SysUserMapper;
import com.yunlbd.flexboot4.metrics.MetricsRecorder;
import com.yunlbd.flexboot4.security.JwtUtil;
import com.yunlbd.flexboot4.security.UserDetailsCacheService;
import com.yunlbd.flexboot4.service.sys.impl.UserSecurityEmailServiceImpl;
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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserSecurityEmailServiceImplTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final SysUserService sysUserService = mock(SysUserService.class);
    private final UserDetailsCacheService userDetailsCacheService = mock(UserDetailsCacheService.class);
    private final EmailService emailService = mock(EmailService.class);
    private final JwtUtil jwtUtil = new JwtUtil();
    private final MetricsRecorder metricsRecorder = mock(MetricsRecorder.class);
    private UserSecurityEmailServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "secret", "thisIsASecretKeyThatIsLongEnoughForHmacSha256SecurityRequirement");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey(any())).thenReturn(false);
        when(valueOperations.increment(any())).thenReturn(1L);
        when(sysUserMapper.selectListByQuery(any(QueryWrapper.class))).thenReturn(List.of());

        service = new UserSecurityEmailServiceImpl(
                redisTemplate,
                sysUserMapper,
                sysUserService,
                userDetailsCacheService,
                objectProvider(emailService),
                jwtUtil,
                metricsRecorder
        );
    }

    @Test
    void sendBindCodeUsesIndependentRedisKeysAndEmailSender() {
        SecurityEmailCodeReq req = new SecurityEmailCodeReq();
        req.setEmail("Alice@Example.com ");

        String message = service.sendBindCode(currentUser(), req);

        assertThat(message).contains("验证码已发送");
        verify(valueOperations).set(
                eq("user:security-email:bind-code:u1:alice@example.com"),
                any(String.class),
                eq(5L),
                eq(TimeUnit.MINUTES)
        );
        verify(valueOperations).set(
                "user:security-email:bind-code:cooldown:u1:alice@example.com",
                "1",
                60L,
                TimeUnit.SECONDS
        );
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerificationCodeEmail(eq("alice@example.com"), codeCaptor.capture(), eq(5));
        assertThat(codeCaptor.getValue()).matches("\\d{6}");
    }

    @Test
    void sendBindCodeRejectsEmailBoundByOtherUser() {
        SysUser other = new SysUser();
        other.setId("u2");
        other.setEmail("alice@example.com");
        when(sysUserMapper.selectListByQuery(any(QueryWrapper.class))).thenReturn(List.of(other));

        SecurityEmailCodeReq req = new SecurityEmailCodeReq();
        req.setEmail("alice@example.com");

        assertThatThrownBy(() -> service.sendBindCode(currentUser(), req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("已被其他账号绑定");
        verify(emailService, never()).sendVerificationCodeEmail(any(), any(), org.mockito.Mockito.anyInt());
    }

    @Test
    void sendBindCodeRejectsMissingEmailService() {
        service = new UserSecurityEmailServiceImpl(
                redisTemplate,
                sysUserMapper,
                sysUserService,
                userDetailsCacheService,
                objectProvider(null),
                jwtUtil,
                metricsRecorder
        );

        SecurityEmailCodeReq req = new SecurityEmailCodeReq();
        req.setEmail("alice@example.com");

        assertThatThrownBy(() -> service.sendBindCode(currentUser(), req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("邮件发送服务未启用");
    }

    @Test
    void bindEmailUpdatesCurrentUserAndClearsCode() {
        SysUser currentUser = currentUser();
        SecurityEmailCodeReq codeReq = new SecurityEmailCodeReq();
        codeReq.setEmail("Alice@Example.com");
        service.sendBindCode(currentUser, codeReq);
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                eq("user:security-email:bind-code:u1:alice@example.com"),
                hashCaptor.capture(),
                eq(5L),
                eq(TimeUnit.MINUTES)
        );
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerificationCodeEmail(eq("alice@example.com"), codeCaptor.capture(), eq(5));

        SecurityEmailBindReq req = new SecurityEmailBindReq();
        req.setEmail("Alice@Example.com");
        req.setCode(codeCaptor.getValue());
        String codeKey = "user:security-email:bind-code:u1:alice@example.com";
        when(valueOperations.get("user:security-email:bind-code:fail:u1:alice@example.com")).thenReturn(null);
        when(valueOperations.get(codeKey)).thenReturn(hashCaptor.getValue());
        when(sysUserService.updateById(any(SysUser.class), eq(true))).thenReturn(true);

        SecurityEmailBindResp resp = service.bindEmail(currentUser, req);

        assertThat(resp.bound()).isTrue();
        assertThat(resp.emailMasked()).isEqualTo("ali***@example.com");
        assertThat(currentUser.getEmail()).isEqualTo("alice@example.com");
        ArgumentCaptor<SysUser> updateCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserService).updateById(updateCaptor.capture(), eq(true));
        assertThat(updateCaptor.getValue().getId()).isEqualTo("u1");
        assertThat(updateCaptor.getValue().getEmail()).isEqualTo("alice@example.com");
        verify(redisTemplate).delete(codeKey);
        verify(redisTemplate, atLeastOnce()).delete("user:security-email:bind-code:fail:u1:alice@example.com");
        verify(userDetailsCacheService).evictUserCache("alice");
    }

    @Test
    void bindEmailRejectsInvalidCode() {
        SecurityEmailBindReq req = new SecurityEmailBindReq();
        req.setEmail("alice@example.com");
        req.setCode("000000");
        when(valueOperations.get("user:security-email:bind-code:fail:u1:alice@example.com")).thenReturn(null);
        when(valueOperations.get("user:security-email:bind-code:u1:alice@example.com")).thenReturn(null);

        assertThatThrownBy(() -> service.bindEmail(currentUser(), req))
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
