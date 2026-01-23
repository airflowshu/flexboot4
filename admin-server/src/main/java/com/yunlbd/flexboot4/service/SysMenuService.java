package com.yunlbd.flexboot4.service;

import com.yunlbd.flexboot4.dto.VueRoute;
import com.yunlbd.flexboot4.entity.SysMenu;

import java.util.List;

public interface SysMenuService extends IExtendedService<SysMenu> {

    /***
     * 功能描述：获取用户菜单权限
     * 1. 超级管理员 (ID="1") ：
     * - 直接获取完整的菜单树，不做任何过滤。
     * - 利用 MyBatis-Flex 的 @Relation 批量加载整树，性能高效。
     * 2. 普通用户 ：
     * - RBAC 过滤 ：
     *      1. 先查询用户拥有的所有菜单 ID 集合（User -> Role -> Menu）。
     *      2. 再查询完整的菜单树（利用缓存，避免每次都全量查库）。
     *      3. 最后在内存中进行 树形剪枝 ：仅保留用户有权限访问的节点及其父路径。
     * - 逻辑严谨 ：如果用户拥有某个子菜单的权限，系统会自动保留其父菜单（作为目录显示），确保菜单树结构完整不断裂。
     * @author <a href="mailto:airflow2015@hotmail.com">Wangts</a>
     * @since 2026/1/7 10:31
     * @param userId 用户id
     * @return 菜单树
     */
    List<VueRoute> getUserMenus(String userId);

    List<String> getPermissionCodes(String userId);
}
