package com.yunlbd.flexboot4.controller.sys;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.OperLog;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.common.enums.BusinessType;
import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.dto.AdminResetPasswordReq;
import com.yunlbd.flexboot4.dto.ForgetPasswordReq;
import com.yunlbd.flexboot4.dto.LoginReq;
import com.yunlbd.flexboot4.dto.LoginResp;
import com.yunlbd.flexboot4.dto.ResetPasswordReq;
import com.yunlbd.flexboot4.security.JwtUtil;
import com.yunlbd.flexboot4.service.sys.IAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User login, token and password reset APIs")
@ApiTagGroup(group = "System")
public class AuthController {

    private static final long TOKEN_VALIDITY_HOURS = 2;

    private final IAuthService authService;

    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    @OperLog(title = "User Login", businessType = BusinessType.LOGIN)
    @PostMapping("/login")
    public ApiResult<LoginResp> login(@Valid @RequestBody LoginReq req,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        String clientIp = JwtUtil.getClientIp(request);
        LoginResp loginResp = authService.login(req, clientIp);

        Cookie cookie = new Cookie("access_token", loginResp.getAccessToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setPath("/");
        cookie.setMaxAge((int) Duration.ofHours(TOKEN_VALIDITY_HOURS).getSeconds());
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);

        return ApiResult.success(loginResp);
    }

    @Operation(summary = "User logout", description = "Invalidate JWT token and clear cookie")
    @OperLog(title = "User Logout", businessType = BusinessType.LOGOUT,
            isSaveRequestData = false, isSaveResponseData = false)
    @RequirePermission(skip = true)
    @PostMapping("/logout")
    public ApiResult<String> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request);

        Cookie cookie = new Cookie("access_token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return ApiResult.success("Logged out successfully");
    }

    @Operation(summary = "Refresh token", description = "Refresh the current JWT token")
    @RequirePermission(skip = true)
    @PostMapping("/refresh")
    public ApiResult<String> refresh(HttpServletRequest request, HttpServletResponse response) {
        String newToken = authService.refreshToken(request);

        Cookie cookie = new Cookie("access_token", newToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setPath("/");
        cookie.setMaxAge((int) Duration.ofHours(TOKEN_VALIDITY_HOURS).getSeconds());
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);

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
}