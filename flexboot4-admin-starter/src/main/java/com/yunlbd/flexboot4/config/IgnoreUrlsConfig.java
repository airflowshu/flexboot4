package com.yunlbd.flexboot4.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "security.ignore")
public class IgnoreUrlsConfig {
    private List<String> urls = new ArrayList<>();

    @PostConstruct
    public void init() {
        log.info("IgnoreUrlsConfig initialized with {} URLs: {}", urls.size(), urls);
    }
}
