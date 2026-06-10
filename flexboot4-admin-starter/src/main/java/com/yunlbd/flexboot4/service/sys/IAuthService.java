package com.yunlbd.flexboot4.service.sys;

import com.yunlbd.flexboot4.dto.*;
import com.yunlbd.flexboot4.entity.sys.SysUser;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface IAuthService {

    /**
     * Get public login method options
     */
    AuthLoginOptions getLoginOptions();

    /**
     * Send SMS login code
     */
    String sendSmsCode(SmsCodeReq req, String clientIp);

    /**
     * Get permission codes for current user
     */
    List<String> getPermissionCodes(HttpServletRequest request);

    /**
     * User login
     */
    LoginResp login(LoginReq req, String clientIp);

    /**
     * Build local login result for a verified user from an external login flow.
     */
    LoginResp loginVerifiedUser(SysUser sysUser, String loginType, String clientIp);

    /**
     * Verify second-factor login challenge
     */
    LoginResp verifyMfa(MfaVerifyReq req, String clientIp);

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
