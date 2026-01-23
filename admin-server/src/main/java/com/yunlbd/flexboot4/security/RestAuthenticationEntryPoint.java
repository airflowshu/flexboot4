package com.yunlbd.flexboot4.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunlbd.flexboot4.common.ApiResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        ApiResult<String> body = ApiResult.error(401, "未认证或令牌无效/过期");
        response.getWriter().write(new ObjectMapper().writeValueAsString(body));
    }
}

