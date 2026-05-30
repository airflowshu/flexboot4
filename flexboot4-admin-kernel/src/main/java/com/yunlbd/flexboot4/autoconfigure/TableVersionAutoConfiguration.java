package com.yunlbd.flexboot4.autoconfigure;

import com.yunlbd.flexboot4.cache.BumpTableVersionAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableAspectJAutoProxy(exposeProxy = true)
public class TableVersionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BumpTableVersionAspect bumpTableVersionAspect() {
        return new BumpTableVersionAspect();
    }
}
