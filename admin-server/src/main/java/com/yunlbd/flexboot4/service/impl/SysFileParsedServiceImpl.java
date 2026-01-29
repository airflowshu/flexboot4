package com.yunlbd.flexboot4.service.impl;

import com.yunlbd.flexboot4.entity.SysFileParsed;
import com.yunlbd.flexboot4.mapper.SysFileParsedMapper;
import com.yunlbd.flexboot4.service.SysFileParsedService;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

@Service
@CacheConfig(cacheNames = "sysFileParsed")
public class SysFileParsedServiceImpl extends BaseServiceImpl<SysFileParsedMapper, SysFileParsed> implements SysFileParsedService {
}

