package com.yunlbd.flexboot4.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.entity.SysDictType;
import com.yunlbd.flexboot4.mapper.SysDictTypeMapper;
import com.yunlbd.flexboot4.service.SysDictTypeService;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
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

    @Override
    @Cacheable(key = "'code:' + #code")
    public String getDictTypeIdByCode(String code) {
        if (code == null) {
            return null;
        }
        QueryWrapper qw = QueryWrapper.create()
                .select("id")
                .from("sys_dict_type")
                .where("code = ?", code)
                .limit(1);
        SysDictType dictType = getOneAs(qw, SysDictType.class);
        return dictType != null ? dictType.getId() : null;
    }

}
