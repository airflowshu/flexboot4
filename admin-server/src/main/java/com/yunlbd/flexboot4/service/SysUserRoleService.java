package com.yunlbd.flexboot4.service;

import com.yunlbd.flexboot4.entity.SysUserRole;

import java.util.List;

public interface SysUserRoleService extends IExtendedService<SysUserRole> {

    /**
     * 为用户分配角色（先清除该用户的所有角色关联，再批量新增）
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     */
    boolean assignRolesToUser(String userId, List<String> roleIds);
}
