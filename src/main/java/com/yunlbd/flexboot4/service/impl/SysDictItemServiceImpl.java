package com.yunlbd.flexboot4.service.impl;

import com.yunlbd.flexboot4.entity.SysDictItem;
import com.yunlbd.flexboot4.mapper.SysDictItemMapper;
import com.yunlbd.flexboot4.service.SysDictItemService;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

/**
 *  服务层实现。
 *
 * @author Wangts
 * @since 1.0.0
 */
@Service
@CacheConfig(cacheNames = "sysDictItem")
public class SysDictItemServiceImpl extends BaseServiceImpl<SysDictItemMapper, SysDictItem>  implements SysDictItemService{

}
