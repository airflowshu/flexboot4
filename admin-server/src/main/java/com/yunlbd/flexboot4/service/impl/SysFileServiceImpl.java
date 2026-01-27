package com.yunlbd.flexboot4.service.impl;

import com.yunlbd.flexboot4.entity.SysFile;
import com.yunlbd.flexboot4.mapper.SysFileMapper;
import com.yunlbd.flexboot4.service.SysFileService;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

@Service
@CacheConfig(cacheNames = "sysFile")
public class SysFileServiceImpl extends BaseServiceImpl<SysFileMapper, SysFile> implements SysFileService {
}

