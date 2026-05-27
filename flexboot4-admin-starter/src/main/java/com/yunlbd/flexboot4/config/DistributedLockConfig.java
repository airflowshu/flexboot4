package com.yunlbd.flexboot4.config;

import com.yunlbd.flexboot4.lock.DistributedLockService;
import com.yunlbd.flexboot4.lock.LockProperties;
import com.yunlbd.flexboot4.lock.RedisDistributedLockService;
import com.yunlbd.flexboot4.metrics.MetricsRecorder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@ConditionalOnClass(StringRedisTemplate.class)
public class DistributedLockConfig {

    @Bean
    @ConditionalOnMissingBean(DistributedLockService.class)
    public DistributedLockService redisDistributedLockService(StringRedisTemplate redisTemplate,
                                                             LockProperties lockProperties,
                                                             MetricsRecorder metricsRecorder) {
        return new RedisDistributedLockService(redisTemplate, lockProperties, metricsRecorder);
    }
}
