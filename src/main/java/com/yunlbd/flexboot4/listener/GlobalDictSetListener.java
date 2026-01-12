package com.yunlbd.flexboot4.listener;

import com.mybatisflex.annotation.SetListener;
import com.yunlbd.flexboot4.excel.DictTextResolver;
import com.yunlbd.flexboot4.excel.ExcelDict;

public class GlobalDictSetListener implements SetListener {
    @Override
    public Object onSet(Object entity, String property, Object value) {
        try {
            var field = entity.getClass().getDeclaredField(property);
            var dict = field.getAnnotation(ExcelDict.class);
            if (dict != null) {
                String text = DictTextResolver.resolve(dict.value(), value);
                String target = property + "Str";
                try {
                    var strField = entity.getClass().getDeclaredField(target);
                    strField.setAccessible(true);
                    strField.set(entity, text);
                } catch (NoSuchFieldException ignored) {
                }
            }
        } catch (NoSuchFieldException ignored) {
        } catch (IllegalAccessException ignored) {
        }
        return value;
    }
}
