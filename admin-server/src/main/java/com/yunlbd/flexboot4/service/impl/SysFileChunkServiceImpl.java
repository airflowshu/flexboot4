package com.yunlbd.flexboot4.service.impl;

import com.yunlbd.flexboot4.entity.SysFileChunk;
import com.yunlbd.flexboot4.mapper.SysFileChunkMapper;
import com.yunlbd.flexboot4.service.SysFileChunkService;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

@Service
@CacheConfig(cacheNames = "sysFileChunk")
public class SysFileChunkServiceImpl extends BaseServiceImpl<SysFileChunkMapper, SysFileChunk> implements SysFileChunkService {
}

