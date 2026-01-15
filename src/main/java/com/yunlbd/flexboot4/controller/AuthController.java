package com.yunlbd.flexboot4.controller;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.dto.*;
import com.yunlbd.flexboot4.entity.SysUser;
import com.yunlbd.flexboot4.mapper.SysUserMapper;
import com.yunlbd.flexboot4.security.JwtUtil;
import com.yunlbd.flexboot4.security.UserDetailsServiceImpl;
import com.yunlbd.flexboot4.service.EmailService;
import com.yunlbd.flexboot4.service.SysMenuService;
import com.yunlbd.flexboot4.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User Login and Logout")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final SysUserMapper sysUserMapper;
    private final SysMenuService sysMenuService;
    private final UserDetailsServiceImpl userDetailsService;
    private final EmailService emailService;
    private final SysUserService sysUserService;

    private static final String LOGIN_LIMIT_KEY_PREFIX = "auth:limit:";
    private static final String BLACKLIST_KEY_PREFIX = "auth:blacklist:";
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCK_TIME_MINUTES = 15;

    @Operation(summary = "Get User Permission Codes", description = "Fetch permission codes for the current user.")
    @GetMapping("/codes")
    public ApiResult<List<String>> getCodes(HttpServletRequest request) {
        String token = jwtUtil.resolveToken(request);
        if (token == null) {
            return ApiResult.error(401, "Unauthorized");
        }
        String userId = jwtUtil.extractUserId(token);
        List<String> codes = sysMenuService.getPermissionCodes(userId);
        return ApiResult.success(codes);
    }

    @Operation(summary = "User Login", description = "Authenticate user and return JWT token. Sets HTTP-only cookie.")
    @PostMapping("/login")
    public ApiResult<LoginResp> login(@RequestBody LoginReq req, HttpServletRequest request, HttpServletResponse response) {
        String clientIp = JwtUtil.getClientIp(request);
        String limitKey = LOGIN_LIMIT_KEY_PREFIX + req.getUsername() + ":" + clientIp;

        // 1. Rate Limiting Check
        String attemptsStr = redisTemplate.opsForValue().get(limitKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;
        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            log.warn("Login locked for user: {} IP: {}", req.getUsername(), clientIp);
            return ApiResult.error("Too many login attempts. Please try again later.");
        }

        try {
            // 2. Authenticate
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            assert userDetails != null;

            // Fetch actual user details for response
            SysUser sysUser = sysUserMapper.selectOneByQuery(QueryWrapper.create().where(SysUser::getUsername).eq(req.getUsername()));

            // 3. Generate Token with Claims
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
            
            String token = jwtUtil.generateToken(userDetails, sysUser.getId(), roles);

            // 4. Set HTTP-only Cookie
            Cookie cookie = new Cookie("access_token", token);
            cookie.setHttpOnly(true);
            cookie.setSecure(request.isSecure()); // Set to true in prod
            cookie.setPath("/");
            cookie.setMaxAge((int) Duration.ofHours(2).getSeconds());
            cookie.setAttribute("SameSite", "Strict");
            response.addCookie(cookie);

            // 5. Reset Limit
            redisTemplate.delete(limitKey);
            log.info("User logged in successfully: {}", req.getUsername());

            // 6. Build Response
            LoginResp loginResp = new LoginResp();
            loginResp.setId(sysUser.getId());
            loginResp.setUsername(sysUser.getUsername());
            loginResp.setRealName(sysUser.getRealName());
            // loginResp.setPassword(req.getPassword()); // Returning plain password as requested (unsafe)
            loginResp.setRoles(roles);
            loginResp.setAccessToken(token);

            return ApiResult.success(loginResp);

        } catch (Exception e) {
            // Increment failed attempts
            redisTemplate.opsForValue().increment(limitKey);
            redisTemplate.expire(limitKey, LOCK_TIME_MINUTES, TimeUnit.MINUTES);
            log.warn("Login failed for user: {} IP: {} Reason: {}", req.getUsername(), clientIp, e.getMessage());
            throw e;
        }
    }

    @Operation(summary = "User Logout", description = "Invalidate JWT token and clear cookie.")
    @PostMapping("/logout")
    public ApiResult<String> logout(HttpServletRequest request, HttpServletResponse response, @RequestBody(required = false) Map<String, Boolean> body) {
        String token = jwtUtil.resolveToken(request);
        String username = null;
        
        if (token != null) {
            // 1. Add to Blacklist
            long remainingTTL = jwtUtil.extractExpiration(token).getTime() - System.currentTimeMillis();
            if (remainingTTL > 0) {
                redisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + token, "revoked", remainingTTL, TimeUnit.MILLISECONDS);
            }
            
            // Extract username from token to clear user cache
            username = jwtUtil.extractUsername(token);
        }

        // 2. Clear Cookie if requested or default behavior
        boolean withCredentials = body != null && Boolean.TRUE.equals(body.get("withCredentials"));
        // Even if not explicitly requested, we usually clear cookies on logout for safety,
        // but if the frontend sends withCredentials: true, it specifically expects it.
        // Here we clear it regardless to be safe.
        Cookie cookie = new Cookie("access_token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        
        // 3. Clear user cache if username is available
        if (username != null) {
            userDetailsService.evictUserCache(username);
        }

        log.info("User logged out");
        return ApiResult.success("Logged out successfully");
    }

    @Operation(summary = "Refresh Token", description = "Refresh the current JWT token.")
    @PostMapping("/refresh")
    public ApiResult<String> refresh(HttpServletRequest request, HttpServletResponse response) {
        String token = jwtUtil.resolveToken(request);
        if (token == null) {
            return ApiResult.error(401, "No token provided");
        }

        try {
            // Check blacklist
            if (Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + token))) {
                return ApiResult.error(401, "Token is invalid");
            }

            String username = jwtUtil.extractUsername(token);
            if (username == null) {
                return ApiResult.error(401, "Invalid token");
            }

            // Invalidate old token
            long remainingTTL = jwtUtil.extractExpiration(token).getTime() - System.currentTimeMillis();
            if (remainingTTL > 0) {
                redisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + token, "revoked", remainingTTL, TimeUnit.MILLISECONDS);
            }

            // Generate new token
            // We need to load the user to get fresh roles and id
            SysUser sysUser = sysUserMapper.selectOneByQuery(com.mybatisflex.core.query.QueryWrapper.create().where(SysUser::getUsername).eq(username));
            if (sysUser == null) {
                return ApiResult.error(401, "User not found");
            }
            
            // Simplified: we re-fetch roles. In a real app we might cache this or rely on UserDetailsService
            // But here we have SysUserMapper. We need to fetch roles relations.
            SysUser fullUser = sysUserMapper.selectOneWithRelationsByQuery(
                com.mybatisflex.core.query.QueryWrapper.create().where(SysUser::getUsername).eq(username)
            );
            
            List<String> roles = fullUser.getRoles() != null ? 
                fullUser.getRoles().stream().map(com.yunlbd.flexboot4.entity.SysRole::getRoleValue).collect(Collectors.toList()) 
                : java.util.Collections.emptyList();

            // Create a dummy UserDetails for token generation (password is not needed for generation)
            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(username)
                .password("")
                .authorities(roles.toArray(new String[0]))
                .build();

            String newToken = jwtUtil.generateToken(userDetails, fullUser.getId(), roles);

            // Set Cookie
            Cookie cookie = new Cookie("access_token", newToken);
            cookie.setHttpOnly(true);
            cookie.setSecure(request.isSecure());
            cookie.setPath("/");
            cookie.setMaxAge((int) Duration.ofHours(2).getSeconds());
            cookie.setAttribute("SameSite", "Strict");
            response.addCookie(cookie);

            return ApiResult.success(newToken);

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
             return ApiResult.error(401, "Token expired");
        } catch (Exception e) {
             return ApiResult.error(401, "Token refresh failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Forget Password", description = "Send password reset email to the registered email address.")
    @PostMapping("/forget-password")
    public ApiResult<String> forgetPassword(@Valid @RequestBody ForgetPasswordReq req) {
        String email = req.getEmail().toLowerCase().trim();

        // Find user by email
        SysUser user = sysUserMapper.selectOneByQuery(
                QueryWrapper.create().where(SysUser::getEmail).eq(email)
        );

        // Always return success to prevent email enumeration
        if (user == null) {
            log.warn("Password reset requested for non-existent email: {}", email);
            return ApiResult.success("重置链接已发送，请查收邮件");
        }

        // Generate reset token
        String resetToken = UUID.randomUUID().toString().replace("-", "");

        try {
            emailService.sendPasswordResetEmail(email, resetToken, user.getId());
            log.info("Password reset email sent for user: {} ({})", user.getUsername(), email);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", email, e);
            return ApiResult.error("Failed to send reset email");
        }

        return ApiResult.success("重置链接已发送，请查收邮件");
    }

    @Operation(summary = "Reset Password", description = "Reset password using token received via email.")
    @PostMapping("/reset-password")
    public ApiResult<String> resetPassword(@Valid @RequestBody ResetPasswordReq req) {
        String token = req.getToken().trim();
        String newPassword = req.getNewPassword();

        // Get user ID from token
        String userId = emailService.validateResetToken(token);
        if (userId == null) {
            log.warn("Invalid or expired reset token");
            return ApiResult.error("无效或已过期的重置链接");
        }

        // Update password by user ID
        boolean updated = sysUserService.updatePasswordById(userId, newPassword);

        if (!updated) {
            log.error("Failed to update password for user ID: {}", userId);
            return ApiResult.error("密码重置失败");
        }

        // Invalidate the reset token
        emailService.invalidateResetToken(token);

        // Clear user cache to force re-authentication
        SysUser user = sysUserService.getById(userId);
        if (user != null) {
            userDetailsService.evictUserCache(user.getUsername());
        }

        log.info("Password reset successfully for user: {}", user != null ? user.getUsername() : userId);
        return ApiResult.success("密码重置成功，请使用新密码登录");
    }

    @Operation(summary = "Super/Admin Reset User Password", description = "Super/Admin can reset any user's password by user ID.")
    @PostMapping("/admin/reset-password")
    public ApiResult<String> adminResetPassword(@Valid @RequestBody AdminResetPasswordReq req) {
        // Get user info first
        SysUser user = sysUserService.getById(req.getUserId());
        if (user == null) {
            log.error("User not found for ID: {}", req.getUserId());
            return ApiResult.error("用户不存在");
        }

        // Update password
        boolean updated = sysUserService.updatePasswordById(req.getUserId(), req.getNewPassword());

        if (!updated) {
            log.error("Failed to update password for user ID: {}", req.getUserId());
            return ApiResult.error("密码重置失败");
        }

        // Clear user cache to force re-authentication
        userDetailsService.evictUserCache(user.getUsername());

        // Send notification email if mail is configured and user has email
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            try {
                emailService.sendPasswordResetNotification(user.getEmail(), req.getNewPassword());
            } catch (Exception e) {
                log.warn("Failed to send password reset notification email to: {}", user.getEmail());
            }
        }

        log.info("Admin reset password successfully for user ID: {}", req.getUserId());
        return ApiResult.success("密码重置成功");
    }

}
