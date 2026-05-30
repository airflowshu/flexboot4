package com.yunlbd.flexboot4.service.sys.impl;

import com.yunlbd.flexboot4.entity.sys.SysUserMfa;
import com.yunlbd.flexboot4.mapper.SysUserMfaMapper;
import com.yunlbd.flexboot4.service.sys.SysUserMfaService;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

@Service
@CacheConfig(cacheNames = "sysUserMfa")
public class SysUserMfaServiceImpl extends BaseServiceImpl<SysUserMfaMapper, SysUserMfa> implements SysUserMfaService {
}
