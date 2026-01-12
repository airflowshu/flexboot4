package com.yunlbd.flexboot4.config;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.entity.SysDictItem;
import com.yunlbd.flexboot4.entity.table.SysDictItemTableDef;
import com.yunlbd.flexboot4.excel.DictTextResolver;
import com.yunlbd.flexboot4.service.SysDictItemService;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DictConfiguration {
    public DictConfiguration(SysDictItemService dictItemService) {
        DictTextResolver.setProvider((dict, code) -> {
            if (dict == null || code == null) {
                return null;
            }
            String c = String.valueOf(code);
            QueryWrapper qw = QueryWrapper.create()
                    .select(SysDictItemTableDef.SYS_DICT_ITEM.ALL_COLUMNS)
                    .from(SysDictItemTableDef.SYS_DICT_ITEM)
                    .where(SysDictItemTableDef.SYS_DICT_ITEM.TYPE_CODE.eq(dict)
                            .and(SysDictItemTableDef.SYS_DICT_ITEM.ITEM_CODE.eq(c)))
                    .limit(1);
            SysDictItem item = dictItemService.getOneAs(qw, SysDictItem.class);
            return item != null ? item.getItemText() : c;
        });
    }
}
