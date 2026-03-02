package com.yunlbd.flexboot4.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FileParseProperties.class)
@MapperScan("com.yunlbd.flexboot4.mapper")
public class FileParseConfig {
}



