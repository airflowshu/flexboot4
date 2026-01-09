package com.yunlbd.flexboot4.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.dto.RouteMeta;
import com.yunlbd.flexboot4.dto.VueRoute;
import com.yunlbd.flexboot4.entity.SysMenu;
import com.yunlbd.flexboot4.entity.SysRole;
import com.yunlbd.flexboot4.entity.SysRoleMenu;
import com.yunlbd.flexboot4.entity.SysUserRole;
import com.yunlbd.flexboot4.entity.table.SysMenuTableDef;
import com.yunlbd.flexboot4.entity.table.SysRoleMenuTableDef;
import com.yunlbd.flexboot4.entity.table.SysRoleTableDef;
import com.yunlbd.flexboot4.entity.table.SysUserRoleTableDef;
import com.yunlbd.flexboot4.mapper.SysMenuMapper;
import com.yunlbd.flexboot4.mapper.SysUserRoleMapper;
import com.yunlbd.flexboot4.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.yunlbd.flexboot4.common.constant.SysConstant.SYS_SUPER_USER_ID;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "sysMenu")
public class SysMenuServiceImpl extends BaseServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    private final SysUserRoleMapper sysUserRoleMapper;

    @Override
    @Cacheable(key = "'user:' + #userId")
    public List<VueRoute> getUserMenus(String userId) {
        List<SysMenu> fullTree = mapper.selectListWithRelationsByQuery(
                QueryWrapper.create()
                        .where(SysMenu::getStatus).eq(1)
                        .and(SysMenu::getParentId).eq("0") // Fetch roots
                        .orderBy(SysMenu::getOrderNo).asc()
        );
        // 1. Super Admin (userId = "1"): Return all enabled menus
        if (SYS_SUPER_USER_ID.equals(userId)) {
            return buildVueRoutes(fullTree);
        }

        // 2. Regular User: Filter by RBAC (User -> Role -> Menu)
        QueryWrapper accessQuery = QueryWrapper.create()
                .select(SysMenuTableDef.SYS_MENU.ID)
                .from(SysMenu.class)
                .leftJoin(SysRoleMenu.class).on(SysRoleMenuTableDef.SYS_ROLE_MENU.MENU_ID.eq(SysMenuTableDef.SYS_MENU.ID))
                .leftJoin(SysUserRole.class).on(SysUserRoleTableDef.SYS_USER_ROLE.ROLE_ID.eq(SysRoleMenuTableDef.SYS_ROLE_MENU.ROLE_ID))
                .where(SysUserRoleTableDef.SYS_USER_ROLE.USER_ID.eq(userId))
                .and(SysMenu::getStatus).eq(1);
        
        List<String> accessibleMenuIds = mapper.selectListByQueryAs(accessQuery, String.class);

        if (accessibleMenuIds.isEmpty()) {
            return new ArrayList<>();
        }
        return buildVueRoutesWithFilter(fullTree, accessibleMenuIds);
    }


    private List<VueRoute> buildVueRoutesWithFilter(List<SysMenu> menus, List<String> accessibleIds) {
        List<VueRoute> routes = new ArrayList<>();
        for (SysMenu menu : menus) {
            // Check if this menu or any of its children are accessible
            if (isMenuAccessibleOrHasAccessibleChildren(menu, accessibleIds)) {
                VueRoute route = convertToVueRouteWithFilter(menu, accessibleIds);
                if (route != null) {
                    routes.add(route);
                }
            }
        }
        return routes;
    }

    private boolean isMenuAccessibleOrHasAccessibleChildren(SysMenu menu, List<String> accessibleIds) {
        if (accessibleIds == null) {
            return true; // If null, assume super admin or full access
        }
        if (accessibleIds.contains(menu.getId())) {
            return true;
        }
        if (menu.getChildren() != null) {
            for (SysMenu child : menu.getChildren()) {
                if (isMenuAccessibleOrHasAccessibleChildren(child, accessibleIds)) {
                    return true;
                }
            }
        }
        return false;
    }

    private VueRoute convertToVueRouteWithFilter(SysMenu menu, List<String> accessibleIds) {
        // Even if the parent itself isn't in accessibleIds, if it has accessible children, 
        // we usually want to show it (perhaps as a folder).
        // However, standard RBAC usually implies parent is assigned if child is assigned.
        // Here we strictly follow: show if accessible OR has accessible children.
        
        VueRoute route = new VueRoute();
        route.setId(menu.getId());
        route.setPid(menu.getParentId());
        route.setName(menu.getName());
        route.setPath(menu.getPath());
        route.setComponent(sanitizeComponentPath(menu.getComponent()));
        route.setRedirect(menu.getRedirect());
        route.setMeta(getRouteMeta(menu));
        route.setType(menu.getType());
        route.setStatus(menu.getStatus());
        route.setAuthCode(menu.getAuthCode());

        
        List<VueRoute> childrenRoutes = new ArrayList<>();
        if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
            for (SysMenu child : menu.getChildren()) {
                if (child.getStatus() == 1 && (child.getType().equals("catalog") || child.getType().equals("menu"))) {
                    if (isMenuAccessibleOrHasAccessibleChildren(child, accessibleIds)) {
                        VueRoute childRoute = convertToVueRouteWithFilter(child, accessibleIds);
                        if (childRoute != null) {
                            childrenRoutes.add(childRoute);
                        }
                    }
                }
            }
        }
        
        if (!childrenRoutes.isEmpty()) {
            route.setChildren(childrenRoutes);
        } else {
            // If leaf node and not accessible, don't return it
            if (accessibleIds != null && !accessibleIds.contains(menu.getId())) {
                return null;
            }
        }
        
        return route;
    }
    
    // Kept for backward compatibility or direct full tree conversion
    private List<VueRoute> buildVueRoutes(List<SysMenu> menus) {
        // super admin默认也按所有type返回
        List<VueRoute> routes = new ArrayList<>();
        for (SysMenu menu : menus) {
            VueRoute route = convertToVueRouteAll(menu);
            if (route != null) {
                routes.add(route);
            }
        }
        return routes;
    }

    // Helper to unify logic: if accessibleIds is null, it means allow all
    private boolean isAccessible(String menuId, List<String> accessibleIds) {
        return accessibleIds == null || accessibleIds.contains(menuId);
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
                .select(SysMenu::getAuthCode)
                .from(SysMenu.class)
                .leftJoin(SysRoleMenu.class).on(SysRoleMenuTableDef.SYS_ROLE_MENU.MENU_ID.eq(SysMenuTableDef.SYS_MENU.ID))
                .leftJoin(SysRole.class).on(SysRoleTableDef.SYS_ROLE.ID.eq(SysRoleMenuTableDef.SYS_ROLE_MENU.ROLE_ID))
                .leftJoin(SysUserRole.class).on(SysUserRoleTableDef.SYS_USER_ROLE.ROLE_ID.eq(SysRoleTableDef.SYS_ROLE.ID))
                .where(SysUserRoleTableDef.SYS_USER_ROLE.USER_ID.eq(userId))
                .and(SysMenu::getStatus).eq(1)
                .and(SysMenu::getAuthCode).isNotNull()
                .and(SysMenu::getAuthCode).ne(""); // Ensure not empty

        return mapper.selectListByQueryAs(queryWrapper, String.class).stream()
                .distinct()
                .collect(Collectors.toList());
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

    private VueRoute convertToVueRouteAll(SysMenu menu) {
        if (menu.getStatus() != null && menu.getStatus() != 1) {
            return null;
        }
        VueRoute route = new VueRoute();
        route.setId(menu.getId());
        route.setPid(menu.getParentId());
        route.setName(menu.getName());
        route.setPath(menu.getPath());
        route.setComponent(sanitizeComponentPath(menu.getComponent()));
        route.setRedirect(menu.getRedirect());
        route.setMeta(getRouteMeta(menu));
        route.setType(menu.getType());
        route.setStatus(menu.getStatus());
        route.setAuthCode(menu.getAuthCode());

        List<VueRoute> childrenRoutes = new ArrayList<>();
        if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
            for (SysMenu child : menu.getChildren()) {
                VueRoute childRoute = convertToVueRouteAll(child);
                if (childRoute != null) {
                    childrenRoutes.add(childRoute);
                }
            }
        }
        if (!childrenRoutes.isEmpty()) {
            route.setChildren(childrenRoutes);
        }
        return route;
    }

}
