package com.yunlbd.flexboot4.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.entity.SysUser;
import com.yunlbd.flexboot4.mapper.SysUserMapper;
import com.yunlbd.flexboot4.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 *
 * @author Wangts
 * @Project_Name flexboot4
 * @since 2026年01月07日 12:38
 */
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "sysUser")
public class SysUserServiceImpl extends BaseServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final PasswordEncoder passwordEncoder;

    /**
     * Update user password by user ID
     *
     * @param id          user ID
     * @param newPassword new password (will be encoded)
     * @return true if updated successfully
     */
    public boolean updatePasswordById(String id, String newPassword) {
        SysUser user = super.getById(id);
        if (user == null) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        return updateById(user, true);
    }
}
