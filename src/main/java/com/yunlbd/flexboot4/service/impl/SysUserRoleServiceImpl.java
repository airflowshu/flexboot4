package com.yunlbd.flexboot4.service.impl;

import com.yunlbd.flexboot4.entity.SysUserRole;
import com.yunlbd.flexboot4.mapper.SysUserRoleMapper;
import com.yunlbd.flexboot4.service.SysUserRoleService;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@CacheConfig(cacheNames = "sysUserRole")
public class SysUserRoleServiceImpl extends BaseServiceImpl<SysUserRoleMapper, SysUserRole> implements SysUserRoleService {

    //如果是中间表，需要声明其关联影响的每张表，否则缓存将不会被清除
    @Override
    protected Collection<String> extraInvalidateTables() {
        return List.of("sys_user", "sys_role");
    }
}

