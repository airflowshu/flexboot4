package com.yunlbd.flexboot4.common.util;

import com.yunlbd.flexboot4.entity.SysUser;
import com.yunlbd.flexboot4.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    /**
     * Get Authentication
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Get current LoginUser
     */
    public static LoginUser getLoginUser() {
        Authentication authentication = getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            return (LoginUser) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * Get current SysUser
     */
    public static SysUser getSysUser() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getSysUser() : null;
    }

    /**
     * Get current user ID
     */
    public static Long getUserId() {
        SysUser sysUser = getSysUser();
        return sysUser != null ? sysUser.getId() : null;
    }
}
