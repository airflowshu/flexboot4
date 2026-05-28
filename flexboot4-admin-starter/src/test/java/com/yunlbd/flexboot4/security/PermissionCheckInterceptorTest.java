package com.yunlbd.flexboot4.security;

import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.config.IgnoreUrlsConfig;
import com.yunlbd.flexboot4.controller.sys.BaseCrudController;
import com.yunlbd.flexboot4.converter.IdentityCrudMapper;
import com.yunlbd.flexboot4.entity.sys.BaseEntity;
import com.yunlbd.flexboot4.entity.sys.SysUser;
import com.yunlbd.flexboot4.metrics.MetricsRecorder;
import com.yunlbd.flexboot4.service.sys.IExtendedService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PermissionCheckInterceptorTest {

    private final MetricsRecorder metricsRecorder = mock(MetricsRecorder.class);
    private final PermissionCheckInterceptor interceptor = new PermissionCheckInterceptor(ignoreUrls(), metricsRecorder);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anonymousAdminResetPasswordIsRejected() throws Exception {
        MockHttpServletResponse response = preHandle(
                "/api/admin/auth/admin/reset-password",
                AdminEndpoint.class,
                "adminResetPassword"
        );

        assertThat(response.getStatus()).isEqualTo(401);
        verify(metricsRecorder).increment(eq("flexboot4.auth.anonymous_denied"), any());
    }

    @Test
    void userWithoutPermissionIsForbiddenForMonitorStats() throws Exception {
        setLoginUser("user-1", "alice", List.of("sys:oper:log:list"));

        MockHttpServletResponse response = preHandle(
                "/api/admin/monitor/stats",
                AdminEndpoint.class,
                "monitorStats"
        );

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("权限不足");
        verify(metricsRecorder).increment(eq("flexboot4.permission.denied"), any());
    }

    @Test
    void userWithPermissionCanAccessOperLogPage() throws Exception {
        setLoginUser("user-1", "alice", List.of("sys:oper:log:list"));

        boolean allowed = interceptor.preHandle(
                request("/api/admin/oper-log/page"),
                new MockHttpServletResponse(),
                handler(AdminEndpoint.class, "operLogPage")
        );

        assertThat(allowed).isTrue();
    }

    @Test
    void skipPermissionAllowsAuthenticatedUser() throws Exception {
        setLoginUser("user-1", "alice", List.of());

        boolean allowed = interceptor.preHandle(
                request("/api/admin/auth/codes"),
                new MockHttpServletResponse(),
                handler(AdminEndpoint.class, "codes")
        );

        assertThat(allowed).isTrue();
    }

    @Test
    void menuRouteTreeEndpointAllowsAuthenticatedUser() throws Exception {
        setLoginUser("user-1", "alice", List.of());

        boolean allowed = interceptor.preHandle(
                request("/api/admin/menu/all"),
                new MockHttpServletResponse(),
                handler(AdminEndpoint.class, "menuAll")
        );

        assertThat(allowed).isTrue();
    }

    @Test
    void baseCrudControllerCreateUsesAddPermissionCode() throws Exception {
        setLoginUser("user-1", "alice", List.of("test:entity:add"));

        boolean allowed = interceptor.preHandle(
                request("/api/admin/test-entity"),
                new MockHttpServletResponse(),
                handler(TestEntityController.class, "create", Object.class)
        );

        assertThat(allowed).isTrue();
    }

    @Test
    void baseCrudControllerUpdateUsesEditPermissionCode() throws Exception {
        setLoginUser("user-1", "alice", List.of("test:entity:edit"));

        boolean allowed = interceptor.preHandle(
                request("/api/admin/test-entity/1"),
                new MockHttpServletResponse(),
                handler(TestEntityController.class, "update", java.io.Serializable.class, Object.class)
        );

        assertThat(allowed).isTrue();
    }

    @Test
    void baseCrudControllerDeleteUsesDeletePermissionCode() throws Exception {
        setLoginUser("user-1", "alice", List.of("test:entity:delete"));

        boolean allowed = interceptor.preHandle(
                request("/api/admin/test-entity/1"),
                new MockHttpServletResponse(),
                handler(TestEntityController.class, "remove", java.io.Serializable.class)
        );

        assertThat(allowed).isTrue();
    }

    @Test
    void baseCrudControllerExportUsesExportPermissionCode() throws Exception {
        setLoginUser("user-1", "alice", List.of("test:entity:export"));

        boolean allowed = interceptor.preHandle(
                request("/api/admin/test-entity/export"),
                new MockHttpServletResponse(),
                handler(TestEntityController.class, "exportPost", com.yunlbd.flexboot4.dto.SearchDto.class,
                        jakarta.servlet.http.HttpServletRequest.class, jakarta.servlet.http.HttpServletResponse.class)
        );

        assertThat(allowed).isTrue();
    }

    @Test
    void baseCrudControllerImportUsesImportPermissionCode() throws Exception {
        setLoginUser("user-1", "alice", List.of("test:entity:import"));

        boolean allowed = interceptor.preHandle(
                request("/api/admin/test-entity/import"),
                new MockHttpServletResponse(),
                handler(TestEntityController.class, "importExcel")
        );

        assertThat(allowed).isTrue();
    }

    @Test
    void adminEndpointWithoutAnnotationIsForbiddenByDefault() throws Exception {
        setLoginUser("user-1", "alice", List.of("sys:monitor:stats"));

        MockHttpServletResponse response = preHandle(
                "/api/admin/internal/unannotated",
                AdminEndpoint.class,
                "unannotated"
        );

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("权限不足");
    }

    @Test
    void whitelistStillBypassesPermissionCheck() throws Exception {
        boolean allowed = interceptor.preHandle(
                request("/api/admin/auth/login"),
                new MockHttpServletResponse(),
                handler(AdminEndpoint.class, "unannotated")
        );

        assertThat(allowed).isTrue();
    }

    @Test
    void authOptionsAndSmsCodeBypassPermissionCheck() throws Exception {
        boolean optionsAllowed = interceptor.preHandle(
                request("/api/admin/auth/options"),
                new MockHttpServletResponse(),
                handler(AdminEndpoint.class, "unannotated")
        );
        boolean smsCodeAllowed = interceptor.preHandle(
                request("/api/admin/auth/sms-code"),
                new MockHttpServletResponse(),
                handler(AdminEndpoint.class, "unannotated")
        );

        assertThat(optionsAllowed).isTrue();
        assertThat(smsCodeAllowed).isTrue();
    }

    private MockHttpServletResponse preHandle(String uri, Class<?> beanType, String methodName) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        interceptor.preHandle(request(uri), response, handler(beanType, methodName));
        return response;
    }

    private static IgnoreUrlsConfig ignoreUrls() {
        IgnoreUrlsConfig config = new IgnoreUrlsConfig();
        config.setUrls(List.of(
                "/resources/**",
                "/static/**",
                "/api/admin/auth/options",
                "/api/admin/auth/login",
                "/api/admin/auth/sms-code",
                "/api/admin/auth/forget-password",
                "/api/admin/auth/reset-password",
                "/v3/api-docs/**",
                "/scalar/**",
                "/error"
        ));
        return config;
    }

    private static MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }

    private static HandlerMethod handler(Class<?> beanType, String methodName) throws Exception {
        Object bean = beanType.getDeclaredConstructor().newInstance();
        Method method = beanType.getMethod(methodName);
        return new HandlerMethod(bean, method);
    }

    private static HandlerMethod handler(Class<?> beanType, String methodName, Class<?>... parameterTypes) throws Exception {
        Object bean = beanType.getDeclaredConstructor().newInstance();
        Method method = beanType.getMethod(methodName, parameterTypes);
        return new HandlerMethod(bean, method);
    }

    private static void setLoginUser(String userId, String username, List<String> permissions) {
        SysUser sysUser = SysUser.builder()
                .id(userId)
                .username(username)
                .password("{noop}password")
                .status(1)
                .build();
        LoginUser loginUser = new LoginUser(sysUser, List.of(), permissions);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities())
        );
    }

    static class AdminEndpoint {
        @RequirePermission("sys:user:reset-password")
        public void adminResetPassword() {
        }

        @RequirePermission("sys:monitor:stats")
        public void monitorStats() {
        }

        @RequirePermission("sys:oper:log:list")
        public void operLogPage() {
        }

        @RequirePermission(skip = true)
        public void codes() {
        }

        @RequirePermission(skip = true)
        public void menuAll() {
        }

        public void unannotated() {
        }
    }

    static class TestEntity extends BaseEntity {
    }

    static class TestEntityController extends BaseCrudController<IExtendedService<TestEntity>, TestEntity, String, TestEntity, TestEntity, TestEntity, TestEntity> {
        @SuppressWarnings("unchecked")
        TestEntityController() {
            super((IExtendedService<TestEntity>) mock(IExtendedService.class), new IdentityCrudMapper<>());
        }

        @Override
        public Class<TestEntity> getEntityClass() {
            return TestEntity.class;
        }
    }
}
