package com.yunlbd.flexboot4.aigateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Embedding Stream 配置
 */
@Configuration
@EnableConfigurationProperties({
        EmbeddingStreamProperties.class,
        EmbeddingHttpProperties.class,
        R2dbcProperties.class,
        LlmProxyProperties.class,
        RagProperties.class,
        AiRoutingProperties.class
})
public class EmbeddingStreamConfig {
}
