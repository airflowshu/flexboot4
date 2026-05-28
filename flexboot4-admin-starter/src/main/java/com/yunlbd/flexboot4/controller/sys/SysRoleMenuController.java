package com.yunlbd.flexboot4.controller.sys;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.OperLog;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.common.enums.BusinessType;
import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.service.sys.SysRoleMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色菜单关联 控制层。
 *
 * @author yunlbd_wts
 * @since 2026-01-07
 */
@RestController
@RequestMapping("/api/admin/role-menu")
@Tag(name = "权限管理", description = "SysRoleMenu - 角色菜单关联管理")
@ApiTagGroup(group = "系统管理")
public class SysRoleMenuController {

    private final SysRoleMenuService service;

    public SysRoleMenuController(SysRoleMenuService service) {
        this.service = service;
    }

    @Operation(summary = "为角色分配菜单", description = "先清除该角色的所有菜单关联，再批量新增")
    @OperLog(title = "为角色分配菜单", businessType = BusinessType.OTHER)
    @RequirePermission("sys:role:edit")
    @PostMapping("/assign/{roleId}")
    public ApiResult<Boolean> assignMenusToRole(@PathVariable String roleId, @RequestBody List<String> menuIds) {
        return ApiResult.success(service.assignMenusToRole(roleId, menuIds));
    }

    @Operation(summary = "获取角色菜单ID", description = "获取角色已绑定的菜单ID列表")
    @RequirePermission("sys:role:list")
    @GetMapping("/{roleId}")
    public ApiResult<List<String>> listMenuIdsByRoleId(@PathVariable String roleId) {
        return ApiResult.success(service.listMenuIdsByRoleId(roleId));
    }
}
