package com.yunlbd.flexboot4.config;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.entity.SysDictItem;
import com.yunlbd.flexboot4.entity.SysDictType;
import com.yunlbd.flexboot4.entity.table.SysDictItemTableDef;
import com.yunlbd.flexboot4.entity.table.SysDictTypeTableDef;
import com.yunlbd.flexboot4.excel.DictTextResolver;
import com.yunlbd.flexboot4.service.SysDictItemService;
import com.yunlbd.flexboot4.service.SysDictTypeService;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DictConfiguration {
    public DictConfiguration(SysDictItemService dictItemService, SysDictTypeService dictTypeService) {
        DictTextResolver.setProvider((dictTypeCode, code) -> {
            if (dictTypeCode == null || code == null) {
                return null;
            }
            String c = String.valueOf(code);
            // 第一步：通过字典类型的 CODE 找到对应的 ID
            QueryWrapper typeQw = QueryWrapper.create()
                    .select(SysDictTypeTableDef.SYS_DICT_TYPE.ID)
                    .from(SysDictTypeTableDef.SYS_DICT_TYPE)
                    .where(SysDictTypeTableDef.SYS_DICT_TYPE.CODE.eq(dictTypeCode))
                    .limit(1);
            SysDictType dictType = dictTypeService.getOneAs(typeQw, SysDictType.class);

            if (dictType == null || dictType.getId() == null) {
                return c;
            }

            // 第二步：通过字典类型的 ID 和字典项的 CODE 找到字典项
            QueryWrapper qw = QueryWrapper.create()
                    .select(SysDictItemTableDef.SYS_DICT_ITEM.ALL_COLUMNS)
                    .from(SysDictItemTableDef.SYS_DICT_ITEM)
                    .where(SysDictItemTableDef.SYS_DICT_ITEM.TYPE_ID.eq(dictType.getId())
                            .and(SysDictItemTableDef.SYS_DICT_ITEM.ITEM_CODE.eq(c)))
                    .limit(1);
            SysDictItem item = dictItemService.getOneAs(qw, SysDictItem.class);
            return item != null ? item.getItemText() : c;
        });
    }
}
