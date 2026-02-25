package com.yunlbd.flexboot4.service.ops.impl;

import com.yunlbd.flexboot4.entity.ops.SysVersionLog;
import com.yunlbd.flexboot4.mapper.SysVersionLogMapper;
import com.yunlbd.flexboot4.service.ops.SysVersionLogService;
import com.yunlbd.flexboot4.service.sys.impl.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "sysVersionLog")
public class SysVersionLogServiceImpl extends BaseServiceImpl<SysVersionLogMapper, SysVersionLog> implements SysVersionLogService {

}
