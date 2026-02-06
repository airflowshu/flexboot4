package com.yunlbd.flexboot4.service.sys.impl;

import com.yunlbd.flexboot4.entity.kb.SysFileParsed;
import com.yunlbd.flexboot4.mapper.SysFileParsedMapper;
import com.yunlbd.flexboot4.service.kb.SysFileParsedService;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

@Service
@CacheConfig(cacheNames = "sysFileParsed")
public class SysFileParsedServiceImpl extends BaseServiceImpl<SysFileParsedMapper, SysFileParsed> implements SysFileParsedService {
}

