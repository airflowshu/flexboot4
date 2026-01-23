package com.yunlbd.flexboot4.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.entity.SysRoleMenu;
import com.yunlbd.flexboot4.entity.table.SysRoleMenuTableDef;
import com.yunlbd.flexboot4.mapper.SysRoleMenuMapper;
import com.yunlbd.flexboot4.service.SysRoleMenuService;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@CacheConfig(cacheNames = "sysRoleMenu")
public class SysRoleMenuServiceImpl extends BaseServiceImpl<SysRoleMenuMapper, SysRoleMenu> implements SysRoleMenuService {

    //如果是中间表，需要声明其关联影响的每张表，否则缓存将不会被清除
    @Override
    protected Collection<String> extraInvalidateTables() {
        return List.of("sys_role", "sys_menu");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignMenusToRole(String roleId, List<String> menuIds) {
        // 清除该角色的所有菜单关联
        QueryWrapper wrapper = QueryWrapper.create()
                .where(SysRoleMenuTableDef.SYS_ROLE_MENU.ROLE_ID.eq(roleId));
        super.remove(wrapper);

        // 批量新增菜单关联
        List<SysRoleMenu> roleMenus = menuIds.stream()
                .map(menuId -> SysRoleMenu.builder()
                        .roleId(roleId)
                        .menuId(menuId)
                        .build()).collect(Collectors.toUnmodifiableList());
        super.saveBatch(roleMenus);
        return true;
    }
}

