package com.yunlbd.flexboot4.controller;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.util.SecurityUtils;
import com.yunlbd.flexboot4.dto.VueRoute;
import com.yunlbd.flexboot4.entity.SysMenu;
import com.yunlbd.flexboot4.service.SysMenuService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class SysMenuController extends BaseController<SysMenuService, SysMenu, String> {

    @Override
    protected Class<SysMenu> getEntityClass() {
        return SysMenu.class;
    }

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

    @Operation(summary = "菜单名称是否存在", description = "根据名称检测是否存在其他菜单，更新时可排除自身ID")
    @GetMapping("/name-exists")
    public ApiResult<Boolean> isMenuNameExists(@RequestParam("name") String name,
                                               @RequestParam(value = "id", required = false) String id) {
        QueryWrapper query = QueryWrapper.create()
                .where(SysMenu::getName).eq(name);
        if (id != null && !id.isBlank()) {
            query.and(SysMenu::getId).ne(id);
        }
        return ApiResult.success(sysMenuService.count(query) > 0);
    }

    @Operation(summary = "菜单路径是否存在", description = "根据路径检测是否存在其他菜单，更新时可排除自身ID")
    @GetMapping("/path-exists")
    public ApiResult<Boolean> isMenuPathExists(@RequestParam("path") String path,
                                               @RequestParam(value = "id", required = false) String id) {
        QueryWrapper query = QueryWrapper.create()
                .where(SysMenu::getPath).eq(path);
        if (id != null && !id.isBlank()) {
            query.and(SysMenu::getId).ne(id);
        }
        return ApiResult.success(sysMenuService.count(query) > 0);
    }

}
