package com.yunlbd.flexboot4.service;

import com.mybatisflex.core.service.IService;
import com.yunlbd.flexboot4.dto.VueRoute;
import com.yunlbd.flexboot4.entity.SysMenu;

import java.util.List;
import java.util.Map;

public interface SysMenuService extends IService<SysMenu> {
    List<VueRoute> getUserMenus(Long userId);
    List<String> getPermissionCodes(Long userId);
}
