package com.yunlbd.flexboot4.service;

import com.yunlbd.flexboot4.dto.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface IAuthService {

    /**
     * Get permission codes for current user
     */
    List<String> getPermissionCodes(HttpServletRequest request);

    /**
     * User login
     */
    LoginResp login(LoginReq req, String clientIp);

    /**
     * User logout
     */
    void logout(HttpServletRequest request);

    /**
     * Refresh token
     */
    String refreshToken(HttpServletRequest request);

    /**
     * Send password reset email
     */
    String forgetPassword(ForgetPasswordReq req);

    /**
     * Reset password with token
     */
    String resetPassword(ResetPasswordReq req);

    /**
     * Admin reset user password
     */
    String adminResetPassword(AdminResetPasswordReq req);
}
