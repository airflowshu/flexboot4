package com.yunlbd.flexboot4.controller;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.util.SecurityUtils;
import com.yunlbd.flexboot4.dto.VueRoute;
import com.yunlbd.flexboot4.entity.SysMenu;
import com.yunlbd.flexboot4.service.SysMenuService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class SysMenuController extends BaseController<SysMenuService, SysMenu, String> {

    private final SysMenuService sysMenuService;

    @Operation(summary = "获取菜单权限", description = "获取登录认证用户所拥有的所有菜单访问数据")
    @GetMapping("/all")
    public ApiResult<List<VueRoute>> getAllMenus() {
        String userId = SecurityUtils.getUserId();
        if (userId == null) {
            userId = "0"; // Fallback or handle unauthenticated case
        }
        return ApiResult.success(sysMenuService.getUserMenus(userId));
    }

    @Override
    protected Class<SysMenu> getEntityClass() {
        return SysMenu.class;
    }
}
