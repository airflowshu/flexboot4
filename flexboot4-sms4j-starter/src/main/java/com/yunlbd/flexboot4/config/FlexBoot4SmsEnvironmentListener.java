package com.yunlbd.flexboot4.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.List;

/**
 * Flexboot4 SMS4J Starter 环境监听器
 * <p>
 * 自动加载 {@code flexboot4-sms4j-defaults.yml}，无需外部项目配置 spring.config.import。
 * 核心效果：将 sms4j 的 yaml 文件配置方式关闭，改由数据库动态数据源驱动。
 * </p>
 */
@Slf4j
public class FlexBoot4SmsEnvironmentListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final String DEFAULT_CONFIG_FILE = "classpath:flexboot4-sms4j-defaults.yml";
    private static final String PROPERTY_SOURCE_NAME = "flexboot4-sms4j-defaults";

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        try {
            Resource resource = new ClassPathResource("flexboot4-sms4j-defaults.yml");
            if (resource.exists()) {
                YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
                List<PropertySource<?>> sources = loader.load(PROPERTY_SOURCE_NAME, resource);
                // 添加到最后，优先级低于外部项目的 application.yml
                sources.forEach(source -> environment.getPropertySources().addLast(source));
                log.info("✅ Flexboot4 SMS4J 默认配置已自动加载: {}", DEFAULT_CONFIG_FILE);
            } else {
                log.debug("⚠️ Flexboot4 SMS4J 默认配置文件不存在: {}", DEFAULT_CONFIG_FILE);
            }
        } catch (IOException e) {
            log.error("❌ 加载 Flexboot4 SMS4J 默认配置失败", e);
        }
    }
}

