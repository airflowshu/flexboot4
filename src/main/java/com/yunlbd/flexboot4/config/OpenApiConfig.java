package com.yunlbd.flexboot4.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("FlexBoot4 Admin API")
                        .version("v0.0.1")
                        .description("Backend API (suitable for Front web -- Vben Admin"));
        openAPI.addExtension("x-tagGroups", List.of(
                Map.of(
                        "name", "系统管理",
                        "tags", List.of(
                                "认证管理",
                                "用户管理",
                                "部门管理",
                                "角色管理",
                                "菜单管理",
                                "字典管理",
                                "权限管理"
                        )
                )
        ));
        return openAPI;
    }
}
