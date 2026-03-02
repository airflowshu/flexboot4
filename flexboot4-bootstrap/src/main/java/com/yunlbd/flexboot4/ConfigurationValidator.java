package com.yunlbd.flexboot4;

import com.yunlbd.flexboot4.config.IgnoreUrlsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 配置验证器 - 启动时验证配置是否正确加载
 */
// @Component
public class ConfigurationValidator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationValidator.class);

    private final IgnoreUrlsConfig ignoreUrlsConfig;

    public ConfigurationValidator(IgnoreUrlsConfig ignoreUrlsConfig) {
        this.ignoreUrlsConfig = ignoreUrlsConfig;
    }

    @Override
    public void run(String... args) {
        log.info("=".repeat(80));
        log.info("配置验证开始");
        log.info("=".repeat(80));

        log.info("Security Ignore URLs 数量: {}", ignoreUrlsConfig.getUrls().size());
        log.info("Security Ignore URLs 内容:");
        ignoreUrlsConfig.getUrls().forEach(url -> log.info("  - {}", url));

        if (ignoreUrlsConfig.getUrls().isEmpty()) {
            log.error("❌ 配置加载失败！Security ignore URLs 为空！");
            log.error("请检查 spring.config.import 是否正确配置");
        } else {
            log.info("✅ 配置加载成功！");
        }

        log.info("=".repeat(80));
    }
}


