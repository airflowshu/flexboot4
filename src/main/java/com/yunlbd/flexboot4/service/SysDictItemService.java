package com.yunlbd.flexboot4.service;

import com.yunlbd.flexboot4.entity.SysDictItem;

/**
 *  服务层。
 *
 * @author Wangts
 * @since 1.0.0
 */
public interface SysDictItemService extends IExtendedService<SysDictItem> {

    /**
     * 通过字典类型ID和字典项编码获取字典项文本
     *
     * @param typeId 字典类型ID
     * @param itemCode 字典项编码
     * @return 字典项文本，如果不存在返回null
     */
    String getDictItemText(String typeId, String itemCode);

}
