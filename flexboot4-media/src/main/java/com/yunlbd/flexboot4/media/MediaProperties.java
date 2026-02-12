package com.yunlbd.flexboot4.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "media")
public record MediaProperties(boolean enabled) {
}
