package com.yunlbd.flexboot4.controller.sys;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.entity.SysUserRole;
import com.yunlbd.flexboot4.service.SysUserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-role")
@RequiredArgsConstructor
@Tag(name = "权限管理", description = "SysUserRole - 用户角色关联管理")
public class SysUserRoleController extends BaseController<SysUserRoleService, SysUserRole, String> {

    @Override
    public Class<SysUserRole> getEntityClass() {
        return SysUserRole.class;
    }

    @Operation(summary = "为用户分配角色", description = "先清除该用户的所有角色关联，再批量新增")
    @PostMapping("/assign/{userId}")
    public ApiResult<Boolean> assignRolesToUser(@PathVariable String userId, @RequestBody List<String> roleIds) {
        return ApiResult.success(service.assignRolesToUser(userId, roleIds));
    }

}
