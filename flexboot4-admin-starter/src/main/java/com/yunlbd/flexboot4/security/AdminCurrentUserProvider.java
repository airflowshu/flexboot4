package com.yunlbd.flexboot4.security;

import com.yunlbd.flexboot4.auth.CurrentUser;
import com.yunlbd.flexboot4.auth.CurrentUserProvider;
import com.yunlbd.flexboot4.util.SecurityUtils;
import org.springframework.stereotype.Component;

/**
 * admin-starter 的当前用户提供器实现。
 * <p>
 * 该实现从 Spring Security 的认证上下文中读取 LoginUser，再转换为内核层可识别的
 * CurrentUser。业务模块依赖 CurrentUserProvider 抽象即可获得当前用户信息，
 * 不需要直接感知 SecurityUtils、LoginUser 等安全层细节。
 */
@Component
public class AdminCurrentUserProvider implements CurrentUserProvider {

    @Override
    public CurrentUser currentUser() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null || loginUser.getSysUser() == null) {
            return null;
        }
        return new CurrentUser(
                loginUser.getSysUser().getId(),
                loginUser.getSysUser().getUsername(),
                loginUser.getPermissionCodes()
        );
    }
}
