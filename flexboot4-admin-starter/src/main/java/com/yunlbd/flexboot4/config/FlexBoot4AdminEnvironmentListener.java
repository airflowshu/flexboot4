package com.yunlbd.flexboot4.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.List;

/**
 * Flexboot4 Admin Starter 环境监听器
 * <p>
 * 自动加载 flexboot4-admin-defaults.yml，无需外部项目配置 spring.config.import
 * <p>
 * 工作原理：
 * 1. Spring Boot 启动时发出 ApplicationEnvironmentPreparedEvent
 * 2. 此监听器捕获事件，从 classpath 加载 flexboot4-admin-defaults.yml
 * 3. 将其注册到 Environment 中
 * 4. 优先级低于外部项目的 application.yml，支持覆盖
 */
@Slf4j
public class FlexBoot4AdminEnvironmentListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final String DEFAULT_CONFIG_FILE = "classpath:flexboot4-admin-defaults.yml";
    private static final String PROPERTY_SOURCE_NAME = "flexboot4-admin-defaults";

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();

        try {
            Resource resource = new ClassPathResource("flexboot4-admin-defaults.yml");

            // 如果文件存在才加载
            if (resource.exists()) {
                YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
                List<PropertySource<?>> sources = loader.load(PROPERTY_SOURCE_NAME, resource);

                // 注册到 Environment 中
                // 注意：添加到最后，让外部项目的配置优先级更高
                sources.forEach(source -> environment.getPropertySources().addLast(source));

                log.info("✅ Flexboot4 Admin 默认配置已自动加载: {}", DEFAULT_CONFIG_FILE);
            } else {
                log.debug("⚠️ Flexboot4 Admin 默认配置文件不存在: {}", DEFAULT_CONFIG_FILE);
            }
        } catch (IOException e) {
            log.error("❌ 加载 Flexboot4 Admin 默认配置失败", e);
        }
    }
}

