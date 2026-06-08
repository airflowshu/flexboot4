package com.yunlbd.flexboot4.service.sys.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.dto.VueRoute;
import com.yunlbd.flexboot4.entity.sys.SysMenu;
import com.yunlbd.flexboot4.mapper.SysMenuMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SysMenuServiceImplTest {

    @Test
    void routeParentIsReturnedWhenOnlyHiddenPermissionGroupIsAssigned() {
        SysMenuMapper menuMapper = mock(SysMenuMapper.class);
        SysMenuServiceImpl service = new SysMenuServiceImpl();
        ReflectionTestUtils.setField(service, "mapper", menuMapper);

        SysMenu dictTypeGroup = SysMenu.builder()
                .id("sys_menu_dict_type")
                .parentId("sys_menu_dict")
                .name("SystemDictType")
                .title("system.dict.type")
                .type("menu")
                .hideInMenu(true)
                .authCode("sys:dict:type:list")
                .status(1)
                .children(List.of())
                .build();
        SysMenu dictMenu = SysMenu.builder()
                .id("sys_menu_dict")
                .parentId("sys_menu_system")
                .path("/system/dict")
                .name("SystemDict")
                .component("/system/dict/index")
                .title("system.dict.title")
                .type("menu")
                .status(1)
                .children(List.of(dictTypeGroup))
                .build();
        SysMenu systemMenu = SysMenu.builder()
                .id("sys_menu_system")
                .path("/system")
                .name("System")
                .component("BasicLayout")
                .title("system.title")
                .type("catalog")
                .status(1)
                .children(List.of(dictMenu))
                .build();

        when(menuMapper.selectListWithRelationsByQuery(any(QueryWrapper.class)))
                .thenReturn(List.of(systemMenu));
        when(menuMapper.selectListByQueryAs(any(QueryWrapper.class), eq(String.class)))
                .thenReturn(List.of("sys_menu_dict_type"));

        List<VueRoute> routes = service.getUserMenus("100");

        assertThat(routes).singleElement()
                .extracting(VueRoute::getId)
                .isEqualTo("sys_menu_system");
        assertThat(routes.getFirst().getChildren()).singleElement()
                .satisfies(route -> {
                    assertThat(route.getId()).isEqualTo("sys_menu_dict");
                    assertThat(route.getPath()).isEqualTo("/system/dict");
                    assertThat(route.getChildren()).isNull();
                });
    }

    @Test
    void superUserIdGetsAllPermissionCodes() {
        SysMenuMapper menuMapper = mock(SysMenuMapper.class);
        SysMenuServiceImpl service = new SysMenuServiceImpl();
        ReflectionTestUtils.setField(service, "mapper", menuMapper);

        when(menuMapper.selectListByQueryAs(any(QueryWrapper.class), eq(String.class)))
                .thenReturn(List.of("sys:user:list", "sys:user:add", "sys:user:add"));

        List<String> codes = service.getPermissionCodes("1");

        assertThat(codes).containsExactly("sys:user:list", "sys:user:add");
    }

    @Test
    void superUserIdGetsFullRouteTree() {
        SysMenuMapper menuMapper = mock(SysMenuMapper.class);
        SysMenuServiceImpl service = new SysMenuServiceImpl();
        ReflectionTestUtils.setField(service, "mapper", menuMapper);

        SysMenu userMenu = SysMenu.builder()
                .id("sys_menu_user")
                .parentId("sys_menu_system")
                .path("/system/user")
                .name("SystemUser")
                .component("/system/user/list")
                .title("用户管理")
                .type("menu")
                .status(1)
                .children(List.of())
                .build();
        SysMenu systemMenu = SysMenu.builder()
                .id("sys_menu_system")
                .path("/system")
                .name("System")
                .component("BasicLayout")
                .title("系统管理")
                .type("catalog")
                .status(1)
                .children(List.of(userMenu))
                .build();

        when(menuMapper.selectListWithRelationsByQuery(any(QueryWrapper.class)))
                .thenReturn(List.of(systemMenu));

        List<VueRoute> routes = service.getUserMenus("1");

        assertThat(routes).singleElement()
                .extracting(VueRoute::getId)
                .isEqualTo("sys_menu_system");
        assertThat(routes.getFirst().getChildren()).singleElement()
                .extracting(VueRoute::getId)
                .isEqualTo("sys_menu_user");
    }
}
