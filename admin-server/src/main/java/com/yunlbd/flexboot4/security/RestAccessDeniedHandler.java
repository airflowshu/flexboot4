package com.yunlbd.flexboot4.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunlbd.flexboot4.common.ApiResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        ApiResult<String> body = ApiResult.error(403, "权限不足，禁止访问");
        response.getWriter().write(new ObjectMapper().writeValueAsString(body));
    }
}

