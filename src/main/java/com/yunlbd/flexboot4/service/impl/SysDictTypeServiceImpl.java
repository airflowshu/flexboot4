package com.yunlbd.flexboot4.service.impl;

import com.yunlbd.flexboot4.entity.SysDictType;
import com.yunlbd.flexboot4.mapper.SysDictTypeMapper;
import com.yunlbd.flexboot4.service.SysDictTypeService;
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
public class SysDictTypeServiceImpl extends BaseServiceImpl<SysDictTypeMapper, SysDictType>  implements SysDictTypeService{

}
