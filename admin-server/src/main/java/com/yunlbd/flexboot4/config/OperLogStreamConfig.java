package com.yunlbd.flexboot4.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunlbd.flexboot4.listener.OperLogStreamListener;
import com.yunlbd.flexboot4.service.ops.SysOperLogService;
import com.yunlbd.flexboot4.util.UserAgentService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(OperLogStreamProperties.class)
public class OperLogStreamConfig {

    @Bean
    public OperLogStreamListener operLogStreamListener(StringRedisTemplate stringRedisTemplate,
                                                       SysOperLogService sysOperLogService,
                                                       ObjectMapper objectMapper,
                                                       OperLogStreamProperties properties,
                                                       UserAgentService userAgentService) {
        return new OperLogStreamListener(stringRedisTemplate, sysOperLogService, objectMapper, properties, userAgentService);
    }
}
