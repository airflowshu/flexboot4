package com.yunlbd.flexboot4.service;

import com.yunlbd.flexboot4.entity.SysRoleMenu;

import java.util.List;

public interface SysRoleMenuService extends IExtendedService<SysRoleMenu> {

    /**
     * 为角色分配菜单（先清除该角色的所有菜单关联，再批量新增）
     * @param roleId 角色ID
     * @param menuIds 菜单ID列表
     */
    boolean assignMenusToRole(String roleId, List<String> menuIds);
}

