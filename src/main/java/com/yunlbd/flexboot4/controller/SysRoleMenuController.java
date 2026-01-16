package com.yunlbd.flexboot4.controller;

import com.yunlbd.flexboot4.entity.SysRoleMenu;
import com.yunlbd.flexboot4.service.SysRoleMenuService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 部门表 控制层。
 *
 * @author yunlbd_wts
 * @since 2026-01-07
 */
@RestController
@RequestMapping("/api/role-menu")
public class SysRoleMenuController extends BaseController<SysRoleMenuService, SysRoleMenu, String>  {

    @Override
    public Class<SysRoleMenu> getEntityClass() {
        return SysRoleMenu.class;
    }
}
