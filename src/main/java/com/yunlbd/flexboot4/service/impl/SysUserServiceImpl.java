package com.yunlbd.flexboot4.service.impl;

import com.yunlbd.flexboot4.entity.SysUser;
import com.yunlbd.flexboot4.mapper.SysUserMapper;
import com.yunlbd.flexboot4.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

/**
 *
 * @author Wangts
 * @Project_Name flexboot4
 * @since 2026年01月07日 12:38
 */
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "sysUser")
public class SysUserServiceImpl extends BaseServiceImpl<SysUserMapper, SysUser> implements SysUserService {

}
