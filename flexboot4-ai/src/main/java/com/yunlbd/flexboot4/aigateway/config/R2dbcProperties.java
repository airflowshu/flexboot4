package com.yunlbd.flexboot4.aigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.r2dbc")
public record R2dbcProperties(DataSourceProperties admin, DataSourceProperties vector) {

    public record DataSourceProperties(String host, int port, String database, String username, String password) {
    }
}
