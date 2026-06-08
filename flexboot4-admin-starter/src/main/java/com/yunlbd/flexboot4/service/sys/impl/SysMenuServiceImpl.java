package com.yunlbd.flexboot4.service.sys.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.dto.RouteMeta;
import com.yunlbd.flexboot4.dto.VueRoute;
import com.yunlbd.flexboot4.entity.sys.SysMenu;
import com.yunlbd.flexboot4.entity.sys.SysRole;
import com.yunlbd.flexboot4.entity.sys.SysRoleMenu;
import com.yunlbd.flexboot4.entity.sys.SysUserRole;
import com.yunlbd.flexboot4.entity.sys.table.SysMenuTableDef;
import com.yunlbd.flexboot4.entity.sys.table.SysRoleMenuTableDef;
import com.yunlbd.flexboot4.entity.sys.table.SysRoleTableDef;
import com.yunlbd.flexboot4.entity.sys.table.SysUserRoleTableDef;
import com.yunlbd.flexboot4.mapper.SysMenuMapper;
import com.yunlbd.flexboot4.service.sys.SysMenuService;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.yunlbd.flexboot4.common.constant.SysConstant.SYS_SUPER_USER_ID;

@Service
@CacheConfig(cacheNames = "sysMenu")
public class SysMenuServiceImpl extends BaseServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    private static final Set<String> ROUTE_TYPES = Set.of("catalog", "menu", "embedded", "link");

    //这里不声明使用缓存，一般系统登录后初始会调用一次，使用缓存意义大不，徒增缓存数据一致性的维护成本
    public List<VueRoute> getUserMenus(String userId) {
        // 查询所有状态为启用的菜单。返回给 vben 的路由树只包含路由型节点，按钮节点只用于权限码接口。
        List<SysMenu> fullTree = mapper.selectListWithRelationsByQuery(
                QueryWrapper.create()
                        .where(SysMenu::getStatus).eq(1)
                        .and(SysMenu::getParentId).isNull()
                        .orderBy(SysMenu::getOrderNo).asc()
        );
        // 1. Super Admin: Return all enabled menus
        if (isSuperAdmin(userId)) {
            if (fullTree == null) {
                return List.of();
            }
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
        if (fullTree == null) {
            return List.of();
        }
        return buildVueRoutesWithFilter(fullTree, accessibleMenuIds);
    }


    private List<VueRoute> buildVueRoutesWithFilter(List<SysMenu> menus, List<String> accessibleIds) {
        List<VueRoute> routes = new ArrayList<>();
        for (SysMenu menu : menus) {
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
        if (menu == null || !isEnabled(menu)) {
            return false;
        }
        if (accessibleIds == null) {
            return isRouteType(menu);
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
        if (!isRouteType(menu)) {
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
                if (isMenuAccessibleOrHasAccessibleChildren(child, accessibleIds)) {
                    VueRoute childRoute = convertToVueRouteWithFilter(child, accessibleIds);
                    if (childRoute != null) {
                        childrenRoutes.add(childRoute);
                    }
                }
            }
        }

        if (!childrenRoutes.isEmpty()) {
            route.setChildren(childrenRoutes);
            return route;
        }

        if (accessibleIds != null
                && (accessibleIds.contains(menu.getId()) || hasAccessibleNonRouteDescendant(menu, accessibleIds))) {
            return route;
        }

        // 叶子节点且无权限，不返回
        return null;
    }
    
    private List<VueRoute> buildVueRoutes(List<SysMenu> menus) {
        List<VueRoute> routes = new ArrayList<>();
        for (SysMenu menu : menus) {
            VueRoute route = convertToVueRouteAll(menu);
            if (route != null) {
                routes.add(route);
            }
        }
        return routes;
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
    public List<String> getPermissionCodes(String userId) {
        // 超级管理员返回所有权限码
        if (isSuperAdmin(userId)) {
            return getAllPermissionCodes();
        }

        // 普通用户：通过角色关联查询
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select(SysMenuTableDef.SYS_MENU.AUTH_CODE)
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

    private List<String> getAllPermissionCodes() {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select(SysMenuTableDef.SYS_MENU.AUTH_CODE)
                .from(SysMenu.class)
                .where(SysMenu::getStatus).eq(1)
                .and(SysMenu::getAuthCode).isNotNull()
                .and(SysMenu::getAuthCode).ne("");
        return mapper.selectListByQueryAs(queryWrapper, String.class).stream()
                .distinct()
                .collect(Collectors.toList());
    }

    private boolean isSuperAdmin(String userId) {
        return SYS_SUPER_USER_ID.equals(userId);
    }

    private static RouteMeta getRouteMeta(SysMenu menu) {
        RouteMeta meta = new RouteMeta();
        meta.setTitle(menu.getTitle());
        meta.setIcon(menu.getIcon());
        meta.setActiveIcon(menu.getActiveIcon());
        meta.setHideInMenu(menu.getHideInMenu());
        meta.setOrder(menu.getOrderNo());
        meta.setBadge(menu.getBadge());
        meta.setBadgeType(menu.getBadgeType());
        meta.setBadgeVariants(menu.getBadgeVariants());
        meta.setLink(menu.getLink());
        meta.setIframeSrc(menu.getIframeSrc());
        meta.setAffixTab(menu.getAffixTab());
        meta.setHideChildrenInMenu(menu.getHideChildrenInMenu());
        meta.setHideInBreadcrumb(menu.getHideInBreadcrumb());
        meta.setHideInTab(menu.getHideInTab());
        meta.setKeepAlive(menu.getKeepAlive());
        meta.setMenuVisibleWithForbidden(menu.getMenuVisibleWithForbidden());
        if (menu.getAuthority() != null && !menu.getAuthority().isEmpty()) {
            meta.setAuthority(Arrays.asList(menu.getAuthority().split(",")));
        }
        return meta;
    }

    private VueRoute convertToVueRouteAll(SysMenu menu) {
        if (!isEnabled(menu) || !isRouteType(menu)) {
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

    private boolean isEnabled(SysMenu menu) {
        return menu.getStatus() == null || menu.getStatus() == 1;
    }

    private boolean isRouteType(SysMenu menu) {
        return (menu.getType() == null || ROUTE_TYPES.contains(menu.getType()))
                && menu.getPath() != null
                && !menu.getPath().isBlank();
    }

    private boolean hasAccessibleNonRouteDescendant(SysMenu menu, List<String> accessibleIds) {
        if (menu.getChildren() == null || menu.getChildren().isEmpty() || accessibleIds == null) {
            return false;
        }
        for (SysMenu child : menu.getChildren()) {
            if (!isEnabled(child)) {
                continue;
            }
            if (!isRouteType(child) && accessibleIds.contains(child.getId())) {
                return true;
            }
            if (hasAccessibleNonRouteDescendant(child, accessibleIds)) {
                return true;
            }
        }
        return false;
    }

}
