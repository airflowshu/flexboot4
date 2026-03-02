package com.yunlbd.flexboot4.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DictEnum {
    // 字典枚举,value对应sys_dict_type表的code字段值
    String value();
}

