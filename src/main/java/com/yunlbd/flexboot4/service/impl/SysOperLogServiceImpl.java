package com.yunlbd.flexboot4.service.impl;

import com.yunlbd.flexboot4.entity.SysOperLog;
import com.yunlbd.flexboot4.mapper.SysOperLogMapper;
import com.yunlbd.flexboot4.service.SysOperLogService;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

@Service
@CacheConfig(cacheNames = "sysOperLog")
public class SysOperLogServiceImpl extends BaseServiceImpl<SysOperLogMapper, SysOperLog> implements SysOperLogService {
}
