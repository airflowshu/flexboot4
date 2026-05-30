package com.yunlbd.flexboot4.service.sys.impl;

import com.mybatisflex.core.update.UpdateChain;
import com.yunlbd.flexboot4.common.annotation.BumpTableVersion;
import com.yunlbd.flexboot4.entity.sys.SysUser;
import com.yunlbd.flexboot4.entity.sys.table.SysUserTableDef;
import com.yunlbd.flexboot4.mapper.SysUserMapper;
import com.yunlbd.flexboot4.service.sys.SysUserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
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
        SysUser user = cacheProxy().getById(id);
        if (user == null) {
            return false;
        }
        newPassword = StringUtils.isEmpty(newPassword) ? "11111111" : newPassword;
        user.setPassword(passwordEncoder.encode(newPassword));
        return cacheProxy().updateById(user, true);
    }

    @Override
    @BumpTableVersion(SysUser.class)
    @CacheEvict(allEntries = true, cacheResolver = "dynamicCacheResolver")
    public boolean updateCurrentProfile(String id, String realName, String profileFileId, String remark) {
        SysUserTableDef user = SysUserTableDef.SYS_USER;
        return UpdateChain.of(getMapper())
                .set(user.REAL_NAME, realName, true)
                .set(user.PROFILE_FILE_ID, profileFileId, true)
                .set(user.REMARK, remark, true)
                .where(user.ID.eq(id))
                .and(user.DEL_FLAG.eq(0))
                .update();
    }
}
