package com.yunlbd.flexboot4.security;

import com.yunlbd.flexboot4.config.IgnoreUrlsConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig implements WebMvcConfigurer {

    @Lazy
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final PermissionCheckInterceptor permissionCheckInterceptor;
    private final IgnoreUrlsConfig ignoreUrlsConfig;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Value("${cms.render.url-prefix:/static/cms-pages}")
    private String cmsRenderUrlPrefix;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        String cmsRenderPattern = normalizeToPattern(cmsRenderUrlPrefix);
        http
                .csrf(AbstractHttpConfigurer::disable) // 禁用 CSRF，解决 POST 请求 403 问题
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/resources/**", "/static/**", "/static/favicon.ico", cmsRenderPattern).permitAll()
                        .requestMatchers(ignoreUrlsConfig.getUrls().toArray(new String[0])).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionCheckInterceptor)
                .addPathPatterns("/api/**"); // 对所有 API 请求进行权限校验
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
        return config.getAuthenticationManager();
    }

    private static String normalizeToPattern(String prefix) {
        String normalized = (prefix == null || prefix.isBlank()) ? "/static/cms-pages" : prefix.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized + "/**";
    }
}
