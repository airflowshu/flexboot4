package com.yunlbd.flexboot4.security;

import com.yunlbd.flexboot4.auth.CurrentUser;
import com.yunlbd.flexboot4.auth.CurrentUserProvider;
import com.yunlbd.flexboot4.util.SecurityUtils;
import org.springframework.stereotype.Component;

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
