package com.yunlbd.flexboot4.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.dto.RouteMeta;
import com.yunlbd.flexboot4.dto.VueRoute;
import com.yunlbd.flexboot4.entity.*;
import com.yunlbd.flexboot4.entity.table.SysMenuTableDef;
import com.yunlbd.flexboot4.entity.table.SysRoleMenuTableDef;
import com.yunlbd.flexboot4.entity.table.SysRoleTableDef;
import com.yunlbd.flexboot4.entity.table.SysUserRoleTableDef;
import com.yunlbd.flexboot4.mapper.SysMenuMapper;
import com.yunlbd.flexboot4.service.SysMenuService;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@CacheConfig(cacheNames = "sysMenu")
public class SysMenuServiceImpl extends BaseServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Override
    @Cacheable(key = "'user:' + #userId")
    public List<VueRoute> getUserMenus(String userId) {
        // Filter by user roles and exclude buttons (type = 2)
        // Only return Catalog (0) and Menu (1)
        List<SysMenu> allMenus = this.list(QueryWrapper.create()
                .where(SysMenu::getStatus).eq(1)
                .and(SysMenu::getType).in(0, 1) // 0: Catalog, 1: Menu
                .orderBy("order_no", true));
        return buildMenuTree(allMenus, "0");
    }

    private String sanitizeComponentPath(String component) {
        if (component == null) {
            return null;
        }
        String result = component;
        if (result.startsWith("views/")) {
            result = result.substring(6);
        }
        if (result.endsWith(".vue")) {
            result = result.substring(0, result.length() - 4);
        }
        if (!result.startsWith("/") && !result.equals("BasicLayout") && !result.equals("BlankLayout")) {
             result = "/" + result;
        }
        return result;
    }

    @Override
    @Cacheable(key = "'codes:' + #userId")
    public List<String> getPermissionCodes(String userId) {
        // Query: SysMenu -> SysRoleMenu -> SysRole -> SysUserRole -> SysUser
        // Join tables to get permissions for the specific user
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select(SysMenu::getPermission)
                .from(SysMenu.class)
                .leftJoin(SysRoleMenu.class).on(SysRoleMenuTableDef.SYS_ROLE_MENU.MENU_ID.eq(SysMenuTableDef.SYS_MENU.ID))
                .leftJoin(SysRole.class).on(SysRoleTableDef.SYS_ROLE.ID.eq(SysRoleMenuTableDef.SYS_ROLE_MENU.ROLE_ID))
                .leftJoin(SysUserRole.class).on(SysUserRoleTableDef.SYS_USER_ROLE.ROLE_ID.eq(SysRoleTableDef.SYS_ROLE.ID))
                .where(SysUserRole::getUserId).eq(userId)
                .and(SysMenu::getStatus).eq(1)
                .and(SysMenu::getPermission).isNotNull()
                .and(SysMenu::getPermission).ne(""); // Ensure not empty

        return mapper.selectListByQueryAs(queryWrapper, String.class).stream()
                .distinct()
                .collect(Collectors.toList());
    }

    private List<VueRoute> buildMenuTree(List<SysMenu> menus, String parentId) {
        List<VueRoute> tree = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (Objects.equals(menu.getParentId(), parentId)) {
                VueRoute node = new VueRoute();
                node.setId(menu.getId());
                node.setPid(menu.getParentId());
                node.setPath(menu.getPath());
                node.setName(menu.getName());
                node.setComponent(sanitizeComponentPath(menu.getComponent()));
                node.setRedirect(menu.getRedirect());
                node.setStatus(menu.getStatus());
                node.setAuthCode(menu.getPermission());

                String typeStr = "menu";
                if (menu.getType() != null) {
                    typeStr = switch (menu.getType()) {
                        case 0 -> "catalog";
                        case 1 -> "menu";
                        case 2 -> "button";
                        case 3 -> "embedded";
                        case 4 -> "link";
                        default -> typeStr;
                    };
                }
                node.setType(typeStr);

                RouteMeta meta = getRouteMeta(menu);
                node.setMeta(meta);
                
                // Recursively build children
                List<VueRoute> children = buildMenuTree(menus, menu.getId());
                if (!children.isEmpty()) {
                    node.setChildren(children);
                }
                
                tree.add(node);
            }
        }
        return tree;
    }

    private static RouteMeta getRouteMeta(SysMenu menu) {
        RouteMeta meta = new RouteMeta();
        meta.setTitle(menu.getTitle());
        meta.setIcon(menu.getIcon());
        meta.setActiveIcon(menu.getActiveIcon());
        meta.setHideMenu(menu.getHideMenu());
        meta.setOrder(menu.getOrderNo());
        meta.setBadge(menu.getBadge());
        meta.setBadgeType(menu.getBadgeType());
        meta.setBadgeVariants(menu.getBadgeVariants());
        meta.setLink(menu.getLink());
        meta.setIframeSrc(menu.getIframeSrc());
        meta.setAffixTab(menu.getAffixTab());
        meta.setHideChildrenInMenu(menu.getHideChildrenInMenu());
        meta.setHideBreadcrumb(menu.getHideBreadcrumb());
        meta.setHideTab(menu.getHideTab());
        meta.setKeepAlive(menu.getKeepAlive());
        meta.setMenuVisibleWithForbidden(menu.getMenuVisibleWithForbidden());
        if (menu.getAuthority() != null && !menu.getAuthority().isEmpty()) {
            meta.setAuthority(Arrays.asList(menu.getAuthority().split(",")));
        }
        return meta;
    }

}
