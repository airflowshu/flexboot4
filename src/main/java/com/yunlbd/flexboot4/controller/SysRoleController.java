package com.yunlbd.flexboot4.controller;

import com.yunlbd.flexboot4.entity.SysRole;
import com.yunlbd.flexboot4.service.SysRoleService;
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
public class SysRoleController extends BaseController<SysRoleService, SysRole, String>  {

    @Override
    protected Class<SysRole> getEntityClass() {
        return SysRole.class;
    }
}
