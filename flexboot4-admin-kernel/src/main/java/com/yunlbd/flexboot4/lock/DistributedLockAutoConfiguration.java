package com.yunlbd.flexboot4.lock;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(LockProperties.class)
public class DistributedLockAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DistributedLockService.class)
    public DistributedLockService noopDistributedLockService() {
        return new NoopDistributedLockService();
    }
}
