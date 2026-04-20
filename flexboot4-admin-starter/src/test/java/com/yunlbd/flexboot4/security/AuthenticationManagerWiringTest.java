package com.yunlbd.flexboot4.security;

import com.yunlbd.flexboot4.config.IgnoreUrlsConfig;
import com.yunlbd.flexboot4.service.sys.SysMenuService;
import com.yunlbd.flexboot4.service.sys.SysUserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticationManagerWiringTest {

    @Test
    void authenticationManagerBeanExists() throws Exception {
        AuthenticationConfiguration authenticationConfiguration = mock(AuthenticationConfiguration.class);
        AuthenticationManager expected = mock(AuthenticationManager.class);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(expected);

        IgnoreUrlsConfig ignoreUrlsConfig = new IgnoreUrlsConfig();
        ignoreUrlsConfig.setUrls(List.of("/api/admin/auth/login"));

        SecurityConfig securityConfig = new SecurityConfig(
                mock(JwtAuthenticationFilter.class),
                mock(PermissionCheckInterceptor.class),
                ignoreUrlsConfig,
                mock(RestAuthenticationEntryPoint.class),
                mock(RestAccessDeniedHandler.class)
        );

        AuthenticationManager actual = securityConfig.authenticationManager(authenticationConfiguration);
        assertThat(actual).isSameAs(expected);
    }

    @Test
    void userDetailsServiceCanBeCreated() {
        UserDetailsServiceImpl userDetailsService = new UserDetailsServiceImpl(
                mock(SysUserService.class),
                mock(SysMenuService.class)
        );

        assertThat(userDetailsService).isNotNull();
    }
}