package com.yunlbd.flexboot4.config;

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

            // 第一步：通过字典类型的 CODE 找到对应的 ID（使用缓存）
            String typeId = dictTypeService.getDictTypeIdByCode(dictTypeCode);
            if (typeId == null) {
                return c;
            }

            // 第二步：通过字典类型的 ID 和字典项的 CODE 找到字典项（使用缓存）
            String itemText = dictItemService.getDictItemText(typeId, c);
            return itemText != null ? itemText : c;
        });
    }
}
