package com.yunlbd.flexboot4.controller.sys;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.OperLog;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.common.enums.BusinessType;
import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.dto.AdminResetPasswordReq;
import com.yunlbd.flexboot4.dto.AuthLoginOptions;
import com.yunlbd.flexboot4.dto.ForgetPasswordReq;
import com.yunlbd.flexboot4.dto.LoginReq;
import com.yunlbd.flexboot4.dto.LoginResp;
import com.yunlbd.flexboot4.dto.MfaVerifyReq;
import com.yunlbd.flexboot4.dto.ResetPasswordReq;
import com.yunlbd.flexboot4.dto.SmsCodeReq;
import com.yunlbd.flexboot4.security.AccessTokenResponseWriter;
import com.yunlbd.flexboot4.security.JwtUtil;
import com.yunlbd.flexboot4.service.sys.IAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User login, token and password reset APIs")
@ApiTagGroup(group = "System")
public class AuthController {

    private final IAuthService authService;
    private final AccessTokenResponseWriter accessTokenResponseWriter;

    @Operation(summary = "Login options", description = "Fetch public login method options")
    @RequirePermission(skip = true)
    @GetMapping("/options")
    public ApiResult<AuthLoginOptions> options() {
        return ApiResult.success(authService.getLoginOptions());
    }

    @Operation(summary = "Send SMS code", description = "Send SMS verification code for phone login")
    @RequirePermission(skip = true)
    @PostMapping("/sms-code")
    public ApiResult<String> sendSmsCode(@Valid @RequestBody SmsCodeReq req, HttpServletRequest request) {
        return ApiResult.success(authService.sendSmsCode(req, JwtUtil.getClientIp(request)));
    }

    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    @OperLog(title = "User Login", businessType = BusinessType.LOGIN)
    @PostMapping("/login")
    public ApiResult<LoginResp> login(@Valid @RequestBody LoginReq req,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        String clientIp = JwtUtil.getClientIp(request);
        LoginResp loginResp = authService.login(req, clientIp);

        addAccessTokenCookieIfPresent(request, response, loginResp);

        return ApiResult.success(loginResp);
    }

    @Operation(summary = "Verify MFA challenge", description = "Verify second-factor code and return JWT token")
    @OperLog(title = "Verify MFA Login", businessType = BusinessType.LOGIN)
    @RequirePermission(skip = true)
    @PostMapping("/mfa/verify")
    public ApiResult<LoginResp> verifyMfa(@Valid @RequestBody MfaVerifyReq req,
                                          HttpServletRequest request,
                                          HttpServletResponse response) {
        LoginResp loginResp = authService.verifyMfa(req, JwtUtil.getClientIp(request));
        addAccessTokenCookieIfPresent(request, response, loginResp);
        return ApiResult.success(loginResp);
    }

    @Operation(summary = "User logout", description = "Invalidate JWT token and clear cookie")
    @OperLog(title = "User Logout", businessType = BusinessType.LOGOUT,
            isSaveRequestData = false, isSaveResponseData = false)
    @RequirePermission(skip = true)
    @PostMapping("/logout")
    public ApiResult<String> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request);
        accessTokenResponseWriter.clear(response);

        return ApiResult.success("Logged out successfully");
    }

    @Operation(summary = "Refresh token", description = "Refresh the current JWT token")
    @RequirePermission(skip = true)
    @PostMapping("/refresh")
    public ApiResult<String> refresh(HttpServletRequest request, HttpServletResponse response) {
        String newToken = authService.refreshToken(request);
        accessTokenResponseWriter.writeCookie(request, response, newToken);

        return ApiResult.success(newToken);
    }

    @Operation(summary = "Forget password", description = "Send password reset email to registered address")
    @PostMapping("/forget-password")
    public ApiResult<String> forgetPassword(@Valid @RequestBody ForgetPasswordReq req) {
        return ApiResult.success(authService.forgetPassword(req));
    }

    @Operation(summary = "Reset password", description = "Reset password using token from email")
    @PostMapping("/reset-password")
    public ApiResult<String> resetPassword(@Valid @RequestBody ResetPasswordReq req) {
        return ApiResult.success(authService.resetPassword(req));
    }

    @Operation(summary = "Admin trigger reset", description = "Send one-time reset link to target user email")
    @RequirePermission("sys:user:reset-password")
    @PostMapping("/admin/reset-password")
    public ApiResult<String> adminResetPassword(@Valid @RequestBody AdminResetPasswordReq req) {
        return ApiResult.success(authService.adminResetPassword(req));
    }

    @Operation(summary = "Get permission codes", description = "Fetch permission codes for current user")
    @RequirePermission(skip = true)
    @GetMapping("/codes")
    @OperLog(title = "Get Permission Codes", businessType = BusinessType.OTHER)
    public ApiResult<List<String>> getCodes(HttpServletRequest request) {
        List<String> codes = authService.getPermissionCodes(request);
        if (codes == null || codes.isEmpty()) {
            return ApiResult.success(List.of());
        }
        return ApiResult.success(codes);
    }

    private void addAccessTokenCookieIfPresent(HttpServletRequest request,
                                               HttpServletResponse response,
                                               LoginResp loginResp) {
        if (loginResp.getAccessToken() == null || loginResp.getAccessToken().isBlank()) {
            return;
        }
        accessTokenResponseWriter.writeCookie(request, response, loginResp.getAccessToken());
    }
}
