package com.yunlbd.flexboot4.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Flexboot4 Media Starter 环境监听器
 *
 * 自动加载 flexboot4-media-defaults.yml，无需外部项目配置 spring.config.import
 */
public class FlexBoot4MediaEnvironmentListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final Logger log = LoggerFactory.getLogger(FlexBoot4MediaEnvironmentListener.class);
    private static final String DEFAULT_CONFIG_FILE = "classpath:flexboot4-media-defaults.yml";
    private static final String PROPERTY_SOURCE_NAME = "flexboot4-media-defaults";

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();

        try {
            Resource resource = new ClassPathResource("flexboot4-media-defaults.yml");

            if (resource.exists()) {
                YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
                List<PropertySource<?>> sources = loader.load(PROPERTY_SOURCE_NAME, resource);

                sources.forEach(source -> environment.getPropertySources().addLast(source));

                log.info("✅ Flexboot4 Media 默认配置已自动加载: {}", DEFAULT_CONFIG_FILE);
            } else {
                log.debug("⚠️ Flexboot4 Media 默认配置文件不存在: {}", DEFAULT_CONFIG_FILE);
            }
        } catch (IOException e) {
            log.error("❌ 加载 Flexboot4 Media 默认配置失败", e);
        }
    }
}



