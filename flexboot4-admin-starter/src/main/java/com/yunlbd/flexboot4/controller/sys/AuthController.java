package com.yunlbd.flexboot4.controller.sys;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.OperLog;
import com.yunlbd.flexboot4.common.enums.BusinessType;
import com.yunlbd.flexboot4.dto.*;
import com.yunlbd.flexboot4.security.JwtUtil;
import com.yunlbd.flexboot4.service.sys.IAuthService;
import com.yunlbd.flexboot4.config.ApiTagGroup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "Authentication - 用户登录和注销")
@ApiTagGroup(group = "系统管理")
public class AuthController {

    private final IAuthService authService;

    private static final long TOKEN_VALIDITY_HOURS = 2;

    @Operation(summary = "User Login",
            description = "Authenticate user and return JWT token. Sets HTTP-only cookie.",
            tags = {"1. 用户登录"}
    )
    @OperLog(title = "用户登录", businessType = BusinessType.LOGIN)
    @PostMapping("/login")
    public ApiResult<LoginResp> login(@Valid @RequestBody LoginReq req, HttpServletRequest request, HttpServletResponse response) {
        String clientIp = JwtUtil.getClientIp(request);

        LoginResp loginResp = authService.login(req, clientIp);

        // Set HTTP-only Cookie
        Cookie cookie = new Cookie("access_token", loginResp.getAccessToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setPath("/");
        cookie.setMaxAge((int) Duration.ofHours(TOKEN_VALIDITY_HOURS).getSeconds());
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);

        return ApiResult.success(loginResp);
    }

    @Operation(summary = "User Logout", description = "Invalidate JWT token and clear cookie.", tags = {"2. 用户登出"})
    @OperLog(title = "用户登出", businessType = BusinessType.LOGOUT, isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/logout")
    public ApiResult<String> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request);

        // Clear cookie
        Cookie cookie = new Cookie("access_token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return ApiResult.success("Logged out successfully");
    }

    @Operation(summary = "Refresh Token", description = "Refresh the current JWT token.", tags = {"3. 刷新token"})
    @PostMapping("/refresh")
    public ApiResult<String> refresh(HttpServletRequest request, HttpServletResponse response) {
        String newToken = authService.refreshToken(request);

        // Set new cookie
        Cookie cookie = new Cookie("access_token", newToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setPath("/");
        cookie.setMaxAge((int) Duration.ofHours(TOKEN_VALIDITY_HOURS).getSeconds());
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);

        return ApiResult.success(newToken);
    }

    @Operation(summary = "Forget Password",
            description = "Send password reset email to the registered email address.",
            tags = {"4. 忘记密码"}
    )
    @PostMapping("/forget-password")
    public ApiResult<String> forgetPassword(@Valid @RequestBody ForgetPasswordReq req) {
        String result = authService.forgetPassword(req);
        return ApiResult.success(result);
    }

    @Operation(summary = "Reset Password", description = "Reset password using token received via email.",tags = {"5. 重置密码"})
    @PostMapping("/reset-password")
    public ApiResult<String> resetPassword(@Valid @RequestBody ResetPasswordReq req) {
        String result = authService.resetPassword(req);
        return ApiResult.success(result);
    }

    @Operation(summary = "Super/Admin Reset User Password",
            description = "Super/Admin can reset any user's password by user ID.",
            tags = {"6. 管理员重置密码"}
    )
    @PostMapping("/admin/reset-password")
    public ApiResult<String> adminResetPassword(@Valid @RequestBody AdminResetPasswordReq req) {
        String result = authService.adminResetPassword(req);
        return ApiResult.success(result);
    }

    @Operation(summary = "Get User Permission Codes",
            description = "Fetch permission codes for the current user.",
            tags = {"7. 获取权限码"}
    )
    @GetMapping("/codes")
    @OperLog(title = "获取权限码", businessType = BusinessType.OTHER)
    public ApiResult<List<String>> getCodes(HttpServletRequest request) {
        List<String> codes = authService.getPermissionCodes(request);
        if (codes == null || codes.isEmpty()) {
            return ApiResult.success(List.of());
        }
        return ApiResult.success(codes);
    }
}
