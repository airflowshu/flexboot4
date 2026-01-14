package com.yunlbd.flexboot4.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.entity.SysUserRole;
import com.yunlbd.flexboot4.entity.table.SysUserRoleTableDef;
import com.yunlbd.flexboot4.mapper.SysUserRoleMapper;
import com.yunlbd.flexboot4.service.SysUserRoleService;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@CacheConfig(cacheNames = "sysUserRole")
public class SysUserRoleServiceImpl extends BaseServiceImpl<SysUserRoleMapper, SysUserRole> implements SysUserRoleService {

    //如果是中间表，需要声明其关联影响的每张表，否则缓存将不会被清除
    @Override
    protected Collection<String> extraInvalidateTables() {
        return List.of("sys_user", "sys_role");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRolesToUser(String userId, List<String> roleIds) {
        // 清除该用户的所有角色关联
        QueryWrapper wrapper = QueryWrapper.create()
                .where(SysUserRoleTableDef.SYS_USER_ROLE.USER_ID.eq(userId));
        super.remove(wrapper);

        // 批量新增角色关联
        List<SysUserRole> userRoles = roleIds.stream()
                .map(roleId -> SysUserRole.builder()
                        .userId(Long.valueOf(userId))
                        .roleId(Long.valueOf(roleId))
                        .build()).collect(Collectors.toUnmodifiableList());
        super.saveBatch(userRoles);
        return true;
    }
}
