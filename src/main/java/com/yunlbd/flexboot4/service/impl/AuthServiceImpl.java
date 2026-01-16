package com.yunlbd.flexboot4.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.dto.*;
import com.yunlbd.flexboot4.entity.SysRole;
import com.yunlbd.flexboot4.entity.SysUser;
import com.yunlbd.flexboot4.mapper.SysUserMapper;
import com.yunlbd.flexboot4.security.JwtUtil;
import com.yunlbd.flexboot4.security.UserDetailsServiceImpl;
import com.yunlbd.flexboot4.service.EmailService;
import com.yunlbd.flexboot4.service.IAuthService;
import com.yunlbd.flexboot4.service.SysMenuService;
import com.yunlbd.flexboot4.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

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

    @Override
    public List<String> getPermissionCodes(HttpServletRequest request) {
        String token = jwtUtil.resolveToken(request);
        if (token == null) {
            return Collections.emptyList();
        }
        String userId = jwtUtil.extractUserId(token);
        return sysMenuService.getPermissionCodes(userId);
    }

    @Override
    public LoginResp login(LoginReq req, String clientIp) {
        String limitKey = LOGIN_LIMIT_KEY_PREFIX + req.getUsername() + ":" + clientIp;

        // Rate limiting check
        String attemptsStr = redisTemplate.opsForValue().get(limitKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;
        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            log.warn("Login locked for user: {} IP: {}", req.getUsername(), clientIp);
            throw new SecurityException("Too many login attempts. Please try again later.");
        }

        try {
            // Authenticate
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            SysUser sysUser = sysUserMapper.selectOneByQuery(
                    QueryWrapper.create().where(SysUser::getUsername).eq(req.getUsername())
            );

            // Generate token
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
            String token = jwtUtil.generateToken(userDetails, sysUser.getId(), roles);

            // Reset login limit
            redisTemplate.delete(limitKey);
            log.info("User logged in successfully: {}", req.getUsername());

            // Build response
            LoginResp loginResp = new LoginResp();
            loginResp.setId(sysUser.getId());
            loginResp.setUsername(sysUser.getUsername());
            loginResp.setRealName(sysUser.getRealName());
            loginResp.setRoles(roles);
            loginResp.setAccessToken(token);
            return loginResp;

        } catch (Exception e) {
            // Increment failed attempts
            redisTemplate.opsForValue().increment(limitKey);
            redisTemplate.expire(limitKey, LOCK_TIME_MINUTES, TimeUnit.MINUTES);
            log.warn("Login failed for user: {} IP: {} Reason: {}", req.getUsername(), clientIp, e.getMessage());
            throw e;
        }
    }

    @Override
    public void logout(HttpServletRequest request) {
        String token = jwtUtil.resolveToken(request);
        if (token != null) {
            // Add to blacklist
            long remainingTTL = jwtUtil.extractExpiration(token).getTime() - System.currentTimeMillis();
            if (remainingTTL > 0) {
                redisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + token, "revoked", remainingTTL, TimeUnit.MILLISECONDS);
            }

            // Clear user cache
            String username = jwtUtil.extractUsername(token);
            if (username != null) {
                userDetailsService.evictUserCache(username);
            }
        }
        log.info("User logged out");
    }

    @Override
    public String refreshToken(HttpServletRequest request) {
        String token = jwtUtil.resolveToken(request);
        if (token == null) {
            throw new SecurityException("No token provided");
        }

        // Check blacklist
        if (Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + token))) {
            throw new SecurityException("Token is invalid");
        }

        String username = jwtUtil.extractUsername(token);
        if (username == null) {
            throw new SecurityException("Invalid token");
        }

        // Invalidate old token
        long remainingTTL = jwtUtil.extractExpiration(token).getTime() - System.currentTimeMillis();
        if (remainingTTL > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + token, "revoked", remainingTTL, TimeUnit.MILLISECONDS);
        }

        // Get user with roles
        SysUser fullUser = sysUserMapper.selectOneWithRelationsByQuery(
                QueryWrapper.create().where(SysUser::getUsername).eq(username)
        );
        if (fullUser == null) {
            throw new SecurityException("User not found");
        }

        List<String> roles = fullUser.getRoles() != null ?
                fullUser.getRoles().stream().map(SysRole::getRoleValue).collect(Collectors.toList())
                : Collections.emptyList();

        // Generate new token
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(username)
                .password("")
                .authorities(roles.toArray(new String[0]))
                .build();

        return jwtUtil.generateToken(userDetails, fullUser.getId(), roles);
    }

    @Override
    public String forgetPassword(ForgetPasswordReq req) {
        String email = req.getEmail().toLowerCase().trim();

        SysUser user = sysUserMapper.selectOneByQuery(
                QueryWrapper.create().where(SysUser::getEmail).eq(email)
        );

        // Always return success to prevent email enumeration
        if (user == null) {
            log.warn("Password reset requested for non-existent email: {}", email);
            return "重置链接已发送，请查收邮件";
        }

        String resetToken = UUID.randomUUID().toString().replace("-", "");
        emailService.sendPasswordResetEmail(email, resetToken, user.getId());
        log.info("Password reset email sent for user: {} ({})", user.getUsername(), email);

        return "重置链接已发送，请查收邮件";
    }

    @Override
    public String resetPassword(ResetPasswordReq req) {
        String token = req.getToken().trim();
        String newPassword = req.getNewPassword();

        String userId = emailService.validateResetToken(token);
        if (userId == null) {
            log.warn("Invalid or expired reset token");
            throw new SecurityException("无效或已过期的重置链接");
        }

        boolean updated = sysUserService.updatePasswordById(userId, newPassword);
        if (!updated) {
            log.error("Failed to update password for user ID: {}", userId);
            throw new RuntimeException("密码重置失败");
        }

        emailService.invalidateResetToken(token);

        SysUser user = sysUserService.getById(userId);
        if (user != null) {
            userDetailsService.evictUserCache(user.getUsername());
        }

        log.info("Password reset successfully for user: {}", user != null ? user.getUsername() : userId);
        return "密码重置成功，请使用新密码登录";
    }

    @Override
    public String adminResetPassword(AdminResetPasswordReq req) {
        SysUser user = sysUserService.getById(req.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        boolean updated = sysUserService.updatePasswordById(req.getUserId(), req.getNewPassword());
        if (!updated) {
            log.error("Failed to update password for user ID: {}", req.getUserId());
            throw new RuntimeException("密码重置失败");
        }

        userDetailsService.evictUserCache(user.getUsername());

        // Send notification email asynchronously
        emailService.sendPasswordResetNotificationAsync(user.getEmail(), req.getNewPassword());

        log.info("Admin reset password successfully for user ID: {}", req.getUserId());
        return "密码重置成功";
    }
}
