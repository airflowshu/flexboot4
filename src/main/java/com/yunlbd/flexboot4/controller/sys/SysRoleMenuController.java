package com.yunlbd.flexboot4.controller.sys;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.entity.SysRoleMenu;
import com.yunlbd.flexboot4.service.SysRoleMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色菜单关联 控制层。
 *
 * @author yunlbd_wts
 * @since 2026-01-07
 */
@RestController
@RequestMapping("/api/role-menu")
@RequiredArgsConstructor
@Tag(name = "权限管理", description = "SysRoleMenu - 角色菜单关联管理")
public class SysRoleMenuController extends BaseController<SysRoleMenuService, SysRoleMenu, String>  {

    @Override
    public Class<SysRoleMenu> getEntityClass() {
        return SysRoleMenu.class;
    }

    @Operation(summary = "为角色分配菜单", description = "先清除该角色的所有菜单关联，再批量新增")
    @PostMapping("/assign/{roleId}")
    public ApiResult<Boolean> assignMenusToRole(@PathVariable String roleId, @RequestBody List<String> menuIds) {
        return ApiResult.success(service.assignMenusToRole(roleId, menuIds));
    }
}
