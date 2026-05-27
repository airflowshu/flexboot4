package com.yunlbd.flexboot4.metrics;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class MetricsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MetricsRecorder.class)
    public MetricsRecorder noopMetricsRecorder() {
        return new NoopMetricsRecorder();
    }
}
