package com.yunlbd.flexboot4.media;

import com.yunlbd.flexboot4.media.config.MediaRestClientFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(MediaProperties.class)
public class MediaAutoConfiguration {

    @Bean
    public MediaRestClientFactory mediaRestClientFactory() {
        return new MediaRestClientFactory(RestClient.builder());
    }
}
