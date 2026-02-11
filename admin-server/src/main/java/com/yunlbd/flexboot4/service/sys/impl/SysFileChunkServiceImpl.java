package com.yunlbd.flexboot4.service.sys.impl;

import com.yunlbd.flexboot4.entity.kb.SysFileChunk;
import com.yunlbd.flexboot4.mapper.SysFileChunkMapper;
import com.yunlbd.flexboot4.service.kb.SysFileChunkService;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

@Service
@CacheConfig(cacheNames = "sysFileChunk")
public class SysFileChunkServiceImpl extends BaseServiceImpl<SysFileChunkMapper, SysFileChunk> implements SysFileChunkService {
}

