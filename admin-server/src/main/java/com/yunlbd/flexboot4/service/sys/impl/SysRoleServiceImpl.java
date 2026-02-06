package com.yunlbd.flexboot4.service.sys.impl;

import com.yunlbd.flexboot4.entity.sys.SysRole;
import com.yunlbd.flexboot4.mapper.SysRoleMapper;
import com.yunlbd.flexboot4.service.sys.SysRoleService;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

/**
 * 部门表 服务层实现。
 *
 * @author yunlbd_wts
 * @since 2026-01-07
 */
@Service
@CacheConfig(cacheNames = "sysRole")
public class SysRoleServiceImpl extends BaseServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

}
