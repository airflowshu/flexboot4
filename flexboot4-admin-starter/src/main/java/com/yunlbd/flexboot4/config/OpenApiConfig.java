package com.yunlbd.flexboot4.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * OpenAPI 配置类
 *
 * 标签分组现已支持两种方式：
 * 1. 自动扫描：在 Controller 上使用 @ApiTagGroup 注解（推荐）
 * 2. 手动配置：直接在本配置类中定义（备选方案）
 *
 * 扫描器会在应用启动时自动收集所有标记的 Controller，
 * 无需在此处手动维护标签分组列表。
 *
 * @author Flexboot4
 * @since 2026-02
 */
@Slf4j
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("FlexBoot4 Admin API")
                        .version("v0.0.1")
                        .description("Backend API (suitable for Front web # Vben Admin "));

        // 优先使用扫描器自动收集的标签分组
        List<Map<String, Object>> tagGroups = OpenApiTagGroupScanner.getTagGroupsForOpenAPI();

        if (!tagGroups.isEmpty()) {
            openAPI.addExtension("x-tagGroups", tagGroups);
            log.info("OpenAPI tag groups loaded from scanner: {} groups", tagGroups.size());
        } else {
            // 如果扫描器未扫描到任何分组，则使用备选的默认配置
            log.warn("No tag groups found from scanner, using default configuration");
        }

        return openAPI;
    }
}
