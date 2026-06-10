package com.yunlbd.flexboot4.service.sys.impl;

import com.yunlbd.flexboot4.entity.sys.SysUserSocialAccount;
import com.yunlbd.flexboot4.mapper.SysUserSocialAccountMapper;
import com.yunlbd.flexboot4.service.sys.SysUserSocialAccountService;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

@Service
@CacheConfig(cacheNames = "sysUserSocialAccount")
public class SysUserSocialAccountServiceImpl
        extends BaseServiceImpl<SysUserSocialAccountMapper, SysUserSocialAccount>
        implements SysUserSocialAccountService {
}
