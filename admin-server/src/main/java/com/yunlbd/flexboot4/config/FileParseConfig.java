package com.yunlbd.flexboot4.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FileParseProperties.class)
public class FileParseConfig {
}

