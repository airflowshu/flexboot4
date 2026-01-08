package com.yunlbd.flexboot4.service.impl;

import com.yunlbd.flexboot4.entity.SysRole;
import com.yunlbd.flexboot4.mapper.SysRoleMapper;
import com.yunlbd.flexboot4.service.SysRoleService;
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
public class SysRoleServiceImpl extends BaseServiceImpl<SysRoleMapper, SysRole>  implements SysRoleService {

}
