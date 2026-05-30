package com.yunlbd.flexboot4.common.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a custom write method that should invalidate versioned query cache keys.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BumpTableVersion {

    @AliasFor("entities")
    Class<?>[] value() default {};

    @AliasFor("value")
    Class<?>[] entities() default {};

    String[] tables() default {};
}
