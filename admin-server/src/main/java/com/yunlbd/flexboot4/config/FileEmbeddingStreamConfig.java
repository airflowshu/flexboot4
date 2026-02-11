package com.yunlbd.flexboot4.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FileEmbeddingStreamProperties.class)
public class FileEmbeddingStreamConfig {
}

