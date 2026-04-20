package com.yunlbd.flexboot4.service.sys.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.dto.AdminResetPasswordReq;
import com.yunlbd.flexboot4.dto.ForgetPasswordReq;
import com.yunlbd.flexboot4.dto.LoginReq;
import com.yunlbd.flexboot4.dto.LoginResp;
import com.yunlbd.flexboot4.dto.ResetPasswordReq;
import com.yunlbd.flexboot4.entity.sys.SysRole;
import com.yunlbd.flexboot4.entity.sys.SysUser;
import com.yunlbd.flexboot4.mapper.SysUserMapper;
import com.yunlbd.flexboot4.security.JwtUtil;
import com.yunlbd.flexboot4.security.UserDetailsServiceImpl;
import com.yunlbd.flexboot4.service.sys.EmailService;
import com.yunlbd.flexboot4.service.sys.IAuthService;
import com.yunlbd.flexboot4.service.sys.SysMenuService;
import com.yunlbd.flexboot4.service.sys.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.ObjectProvider;
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

    private static final String LOGIN_LIMIT_KEY_PREFIX = "auth:limit:";
    private static final String BLACKLIST_KEY_PREFIX = "auth:blacklist:";
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCK_TIME_MINUTES = 15;
    private static final String RESET_LINK_SENT_MSG = "Reset link sent. Please check your email.";

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final SysUserMapper sysUserMapper;
    private final SysMenuService sysMenuService;
    private final UserDetailsServiceImpl userDetailsService;
    private final ObjectProvider<EmailService> emailServiceProvider;
    private final SysUserService sysUserService;

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

        String attemptsStr = redisTemplate.opsForValue().get(limitKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;
        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            log.warn("Login locked for user: {} IP: {}", req.getUsername(), clientIp);
            throw new SecurityException("Too many login attempts. Please try again later.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            SysUser sysUser = sysUserMapper.selectOneByQuery(
                    QueryWrapper.create().where(SysUser::getUsername).eq(req.getUsername())
            );

            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
            List<String> permissions = sysMenuService.getPermissionCodes(sysUser.getId());
            String token = jwtUtil.generateToken(userDetails, sysUser.getId(), roles, permissions);

            redisTemplate.delete(limitKey);
            log.info("User logged in successfully: {}", req.getUsername());

            LoginResp loginResp = new LoginResp();
            loginResp.setId(sysUser.getId());
            loginResp.setUsername(sysUser.getUsername());
            loginResp.setRealName(sysUser.getRealName());
            loginResp.setRoles(roles);
            loginResp.setAccessToken(token);
            return loginResp;
        } catch (Exception e) {
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
            long remainingTtl = jwtUtil.extractExpiration(token).getTime() - System.currentTimeMillis();
            if (remainingTtl > 0) {
                redisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + token, "revoked", remainingTtl, TimeUnit.MILLISECONDS);
            }

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

        if (Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + token))) {
            throw new SecurityException("Token is invalid");
        }

        String username = jwtUtil.extractUsername(token);
        if (username == null) {
            throw new SecurityException("Invalid token");
        }

        long remainingTtl = jwtUtil.extractExpiration(token).getTime() - System.currentTimeMillis();
        if (remainingTtl > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + token, "revoked", remainingTtl, TimeUnit.MILLISECONDS);
        }

        SysUser fullUser = sysUserMapper.selectOneWithRelationsByQuery(
                QueryWrapper.create().where(SysUser::getUsername).eq(username)
        );
        if (fullUser == null) {
            throw new SecurityException("User not found");
        }

        List<String> roles = fullUser.getRoles() == null
                ? Collections.emptyList()
                : fullUser.getRoles().stream().map(SysRole::getRoleValue).collect(Collectors.toList());
        List<String> permissions = sysMenuService.getPermissionCodes(fullUser.getId());

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(username)
                .password("")
                .authorities(roles.toArray(new String[0]))
                .build();

        return jwtUtil.generateToken(userDetails, fullUser.getId(), roles, permissions);
    }

    @Override
    public String forgetPassword(ForgetPasswordReq req) {
        String email = req.getEmail().toLowerCase().trim();

        SysUser user = sysUserMapper.selectOneByQuery(
                QueryWrapper.create().where(SysUser::getEmail).eq(email)
        );

        if (user == null) {
            log.warn("Password reset requested for non-existent email: {}", email);
            return RESET_LINK_SENT_MSG;
        }

        EmailService emailService = emailServiceProvider.getIfAvailable();
        if (emailService == null) {
            log.warn("Password reset email skipped because mail feature is disabled.");
            return RESET_LINK_SENT_MSG;
        }

        String resetToken = UUID.randomUUID().toString().replace("-", "");
        emailService.sendPasswordResetEmail(email, resetToken, user.getId());
        log.info("Password reset email sent for user: {} ({})", user.getUsername(), email);

        return RESET_LINK_SENT_MSG;
    }

    @Override
    public String resetPassword(ResetPasswordReq req) {
        String token = req.getToken().trim();
        String newPassword = req.getNewPassword();

        EmailService emailService = requireEmailService();
        String userId = emailService.validateResetToken(token);
        if (userId == null) {
            log.warn("Invalid or expired reset token");
            throw new SecurityException("Invalid or expired reset link");
        }

        boolean updated = sysUserService.updatePasswordById(userId, newPassword);
        if (!updated) {
            log.error("Failed to update password for user ID: {}", userId);
            throw new RuntimeException("Password reset failed");
        }

        emailService.invalidateResetToken(token);

        SysUser user = sysUserService.getById(userId);
        if (user != null) {
            userDetailsService.evictUserCache(user.getUsername());
        }

        log.info("Password reset successfully for user: {}", user == null ? userId : user.getUsername());
        return "Password reset successfully";
    }

    @Override
    public String adminResetPassword(AdminResetPasswordReq req) {
        EmailService emailService = requireEmailService();
        SysUser user = sysUserService.getById(req.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("User email is required for password reset");
        }

        String resetToken = UUID.randomUUID().toString().replace("-", "");
        emailService.sendPasswordResetEmail(user.getEmail(), resetToken, user.getId());

        log.info("Admin sent password reset link for user ID: {}", req.getUserId());
        return RESET_LINK_SENT_MSG;
    }

    private EmailService requireEmailService() {
        EmailService emailService = emailServiceProvider.getIfAvailable();
        if (emailService == null) {
            throw new IllegalStateException("Mail feature is disabled. Set flexboot4.mail.enabled=true to enable password reset flow.");
        }
        return emailService;
    }
}
