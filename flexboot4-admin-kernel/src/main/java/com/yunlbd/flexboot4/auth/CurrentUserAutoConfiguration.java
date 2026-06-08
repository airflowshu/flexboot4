package com.yunlbd.flexboot4.auth;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class CurrentUserAutoConfiguration {

    /**
     * 条件装配默认实现。
     * <p>
     * 如果业务侧已经提供了 CurrentUserProvider，例如 admin-starter 中的
     * AdminCurrentUserProvider，这个 Noop 实现不会生效。
     */
    @Bean
    @ConditionalOnMissingBean(CurrentUserProvider.class)
    public CurrentUserProvider noopCurrentUserProvider() {
        return new NoopCurrentUserProvider();
    }
}
