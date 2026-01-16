package com.yunlbd.flexboot4.common.annotation;

import java.lang.annotation.*;

/**
 * 接口权限注解
 * 标注在 Controller 方法上，指定访问该接口所需的权限码
 *
 * @usage
 * // 自定义权限码
 * @RequirePermission("sys:user:resetPwd")
 * @PostMapping("/reset-password")
 * public ApiResult<?> resetPassword() { ... }
 *
 * // 跳过权限校验（特殊情况）
 * @RequirePermission(skip = true)
 * @GetMapping("/public")
 * public ApiResult<?> publicApi() { ... }
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /** 所需权限码 */
    String value() default "";

    /** 跳过权限校验（用于特殊情况，如公开接口） */
    boolean skip() default false;
}
