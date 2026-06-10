package com.yunlbd.flexboot4.controller.sys;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.OperLog;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.common.enums.BusinessType;
import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.dto.LoginResp;
import com.yunlbd.flexboot4.dto.oauth.OAuthBindReq;
import com.yunlbd.flexboot4.dto.oauth.OAuthCallbackResultResp;
import com.yunlbd.flexboot4.dto.oauth.OAuthCallbackStatus;
import com.yunlbd.flexboot4.security.AccessTokenResponseWriter;
import com.yunlbd.flexboot4.security.JwtUtil;
import com.yunlbd.flexboot4.service.sys.SocialAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/admin/auth/oauth")
@RequiredArgsConstructor
@Tag(name = "OAuth Authentication", description = "Third-party login and binding APIs")
@ApiTagGroup(group = "System")
public class AuthOAuthController {

    private final SocialAuthService socialAuthService;
    private final AccessTokenResponseWriter accessTokenResponseWriter;

    @Operation(summary = "OAuth authorize", description = "Redirect to third-party authorization page")
    @RequirePermission(skip = true)
    @GetMapping("/{provider}/authorize")
    public ResponseEntity<Void> authorize(@PathVariable String provider, HttpServletRequest request) {
        String authorizeUrl = socialAuthService.buildAuthorizeUrl(provider, request);
        return ResponseEntity.status(302)
                .location(URI.create(authorizeUrl))
                .build();
    }

    @Operation(summary = "OAuth callback", description = "Handle third-party authorization callback")
    @RequirePermission(skip = true)
    @GetMapping("/{provider}/callback")
    public ResponseEntity<Void> callback(@PathVariable String provider,
                                         @RequestParam(required = false) String code,
                                         @RequestParam(required = false) String state,
                                         HttpServletRequest request) {
        String frontendUrl = socialAuthService.handleCallback(provider, code, state, request);
        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, frontendUrl)
                .build();
    }

    @Operation(summary = "OAuth callback result", description = "Consume one-time OAuth callback result")
    @RequirePermission(skip = true)
    @GetMapping("/result/{ticket}")
    public ApiResult<OAuthCallbackResultResp> result(@PathVariable String ticket,
                                                     HttpServletRequest request,
                                                     HttpServletResponse response) {
        OAuthCallbackResultResp result = socialAuthService.consumeResult(ticket);
        writeCookieIfLoginSuccess(result, request, response);
        return ApiResult.success(result);
    }

    @Operation(summary = "Bind OAuth account", description = "Bind third-party identity to an existing local user")
    @OperLog(title = "OAuth Bind Login", businessType = BusinessType.LOGIN)
    @RequirePermission(skip = true)
    @PostMapping("/{provider}/bind")
    public ApiResult<LoginResp> bind(@PathVariable String provider,
                                     @Valid @RequestBody OAuthBindReq req,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        LoginResp loginResp = socialAuthService.bind(provider, req, JwtUtil.getClientIp(request));
        if (loginResp.getAccessToken() != null && !loginResp.getAccessToken().isBlank()) {
            accessTokenResponseWriter.writeCookie(request, response, loginResp.getAccessToken());
        }
        return ApiResult.success(loginResp);
    }

    private void writeCookieIfLoginSuccess(OAuthCallbackResultResp result,
                                           HttpServletRequest request,
                                           HttpServletResponse response) {
        if (result == null || result.status() != OAuthCallbackStatus.LOGIN_SUCCESS || result.login() == null) {
            return;
        }
        String accessToken = result.login().getAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }
        accessTokenResponseWriter.writeCookie(request, response, accessToken);
    }
}
