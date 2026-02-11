package com.yunlbd.flexboot4.common.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    String value() default "";

    boolean skip() default false;
}

