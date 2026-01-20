package com.yunlbd.flexboot4.controller.sys;

import com.yunlbd.flexboot4.entity.SysRole;
import com.yunlbd.flexboot4.service.SysRoleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 部门表 控制层。
 *
 * @author yunlbd_wts
 * @since 2026-01-07
 */
@RestController
@RequestMapping("/api/role")
@RequiredArgsConstructor
@Tag(name = "角色管理", description = "SysRole - 角色管理")
public class SysRoleController extends BaseController<SysRoleService, SysRole, String>  {

    @Override
    public Class<SysRole> getEntityClass() {
        return SysRole.class;
    }
}
