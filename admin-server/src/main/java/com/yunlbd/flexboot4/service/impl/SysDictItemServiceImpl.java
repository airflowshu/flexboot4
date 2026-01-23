package com.yunlbd.flexboot4.service.impl;

import com.yunlbd.flexboot4.entity.SysDictItem;
import com.yunlbd.flexboot4.mapper.SysDictItemMapper;
import com.yunlbd.flexboot4.service.SysDictItemService;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 *  服务层实现。
 *
 * @author Wangts
 * @since 1.0.0
 */
@Service
@CacheConfig(cacheNames = "sysDictItem")
public class SysDictItemServiceImpl extends BaseServiceImpl<SysDictItemMapper, SysDictItem>  implements SysDictItemService{

    //如果是中间表，需要声明其关联影响的每张表，否则缓存将不会被清除
    @Override
    protected Collection<String> extraInvalidateTables() {
        return List.of("sys_dict_type");
    }
}
