package com.yunlbd.flexboot4.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "security.ignore")
public class IgnoreUrlsConfig {
    private List<String> urls = new ArrayList<>();
}
