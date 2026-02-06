package com.yunlbd.flexboot4.service.ops.impl;

import com.yunlbd.flexboot4.entity.ops.SysDictType;
import com.yunlbd.flexboot4.mapper.SysDictTypeMapper;
import com.yunlbd.flexboot4.service.ops.SysDictTypeService;
import com.yunlbd.flexboot4.service.sys.impl.BaseServiceImpl;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

/**
 *  服务层实现。
 *
 * @author Wangts
 * @since 1.0.0
 */
@Service
@CacheConfig(cacheNames = "sysDictType")
public class SysDictTypeServiceImpl extends BaseServiceImpl<SysDictTypeMapper, SysDictType> implements SysDictTypeService{

}
