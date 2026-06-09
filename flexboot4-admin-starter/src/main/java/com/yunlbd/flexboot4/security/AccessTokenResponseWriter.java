package com.yunlbd.flexboot4.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccessTokenResponseWriter {

    public static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
    public static final String ACCESS_TOKEN_HEADER_NAME = "X-Access-Token";

    private static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";

    private final JwtUtil jwtUtil;

    public void clear(HttpServletResponse response) {
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE_NAME, null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    public void writeCookie(HttpServletRequest request, HttpServletResponse response, String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }

        addAccessTokenCookie(request, response, accessToken);
    }

    public void writeCookieAndHeader(HttpServletRequest request, HttpServletResponse response, String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }

        response.setHeader(ACCESS_TOKEN_HEADER_NAME, accessToken);
        exposeAccessTokenHeader(response);
        addAccessTokenCookie(request, response, accessToken);
    }

    private void addAccessTokenCookie(HttpServletRequest request, HttpServletResponse response, String accessToken) {
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE_NAME, accessToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setPath("/");
        cookie.setMaxAge(Math.toIntExact(jwtUtil.getExpirationSeconds()));
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private void exposeAccessTokenHeader(HttpServletResponse response) {
        String exposedHeaders = response.getHeader(ACCESS_CONTROL_EXPOSE_HEADERS);
        if (exposedHeaders == null || exposedHeaders.isBlank()) {
            response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, ACCESS_TOKEN_HEADER_NAME);
            return;
        }
        if (!exposedHeaders.toLowerCase().contains(ACCESS_TOKEN_HEADER_NAME.toLowerCase())) {
            response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, exposedHeaders + "," + ACCESS_TOKEN_HEADER_NAME);
        }
    }
}
