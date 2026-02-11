package com.yunlbd.flexboot4.aigateway.log;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OperLogStreamProperties.class)
public class AiOperLogRedisConfig {
}

