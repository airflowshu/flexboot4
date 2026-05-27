package com.yunlbd.flexboot4.auth;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class CurrentUserAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CurrentUserProvider.class)
    public CurrentUserProvider noopCurrentUserProvider() {
        return new NoopCurrentUserProvider();
    }
}
