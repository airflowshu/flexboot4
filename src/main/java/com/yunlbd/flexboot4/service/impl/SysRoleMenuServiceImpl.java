package com.yunlbd.flexboot4.service.impl;

import com.yunlbd.flexboot4.entity.SysRoleMenu;
import com.yunlbd.flexboot4.mapper.SysRoleMenuMapper;
import com.yunlbd.flexboot4.service.SysRoleMenuService;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@CacheConfig(cacheNames = "sysRoleMenu")
public class SysRoleMenuServiceImpl extends BaseServiceImpl<SysRoleMenuMapper, SysRoleMenu> implements SysRoleMenuService {

    //如果是中间表，需要声明其关联影响的每张表，否则缓存将不会被清除
    @Override
    protected Collection<String> extraInvalidateTables() {
        return List.of("sys_role", "sys_menu");
    }
}

