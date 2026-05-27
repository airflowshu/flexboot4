package com.yunlbd.flexboot4.config;

import java.lang.annotation.*;

/**
 * 用于在 Controller 类上声明 API 标签分组信息
 * 支持自动扫描和注册到 OpenAPI 配置中，无需手动维护 OpenApiConfig
 * 使用示例：
 * {@code
 * @RestController
 * @RequestMapping("/api/admin/user")
 * @Tag(name = "用户管理", description = "用户管理功能")
 * @ApiTagGroup(group = "系统管理")
 * public class SysUserController {
 *     ...
 * }
 * }
 *
 * @author Flexboot4
 * @since 2026-02
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiTagGroup {

    /**
     * 分组名称，必填
     * 例如："系统管理"、"运维管理"、"知识库"
     */
    String group();

    /**
     * 是否启用此分组配置
     * 默认为 true，设为 false 时该 Controller 的标签分组配置将被忽略
     */
    boolean enabled() default true;
}

