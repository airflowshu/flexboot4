package com.yunlbd.flexboot4.service;

import com.yunlbd.flexboot4.entity.SysDictType;

/**
 *  服务层。
 *
 * @author Wangts
 * @since 1.0.0
 */
public interface SysDictTypeService extends IExtendedService<SysDictType> {

    /**
     * 通过字典类型编码获取字典类型ID
     *
     * @param code 字典类型编码
     * @return 字典类型ID，如果不存在返回null
     */
    String getDictTypeIdByCode(String code);

}
