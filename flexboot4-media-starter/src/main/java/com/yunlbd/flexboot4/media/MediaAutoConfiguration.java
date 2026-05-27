package com.yunlbd.flexboot4.media;

import com.yunlbd.flexboot4.media.config.MediaRestClientFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.web.client.RestClient;

@AutoConfiguration
@EnableConfigurationProperties(MediaProperties.class)
@ComponentScan(
        basePackages = {
                "com.yunlbd.flexboot4.controller.media",
                "com.yunlbd.flexboot4.media",
                "com.yunlbd.flexboot4.service.media",
                "com.yunlbd.flexboot4.task.media"
        },
        excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = SpringBootConfiguration.class)
)
public class MediaAutoConfiguration {

    @Bean
    public MediaRestClientFactory mediaRestClientFactory() {
        return new MediaRestClientFactory(RestClient.builder());
    }
}
