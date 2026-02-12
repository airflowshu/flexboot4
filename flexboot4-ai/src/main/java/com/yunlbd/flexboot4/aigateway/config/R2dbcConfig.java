package com.yunlbd.flexboot4.aigateway.config;

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableConfigurationProperties
@EnableR2dbcRepositories(basePackages = "com.yunlbd.flexboot4.aigateway.repository")
public class R2dbcConfig extends AbstractR2dbcConfiguration {

    private final R2dbcProperties r2dbcProperties;

    public R2dbcConfig(R2dbcProperties r2dbcProperties) {
        this.r2dbcProperties = r2dbcProperties;
    }

    @Override
    public ConnectionFactory connectionFactory() {
        return adminConnectionFactory();
    }

    /**
     * Admin 数据源（用于读取 sys_file_chunk）
     */
    @Bean
    public ConnectionFactory adminConnectionFactory() {
        R2dbcProperties.DataSourceProperties admin = r2dbcProperties.admin();
        return new PostgresqlConnectionFactory(
                PostgresqlConnectionConfiguration.builder()
                        .host(admin.host())
                        .port(admin.port())
                        .database(admin.database())
                        .username(admin.username())
                        .password(admin.password())
                        .build()
        );
    }

    /**
     * Vector 数据源（用于写入 ai_vector_chunk）
     */
    @Bean
    public ConnectionFactory vectorConnectionFactory() {
        R2dbcProperties.DataSourceProperties vector = r2dbcProperties.vector();
        return new PostgresqlConnectionFactory(
                PostgresqlConnectionConfiguration.builder()
                        .host(vector.host())
                        .port(vector.port())
                        .database(vector.database())
                        .username(vector.username())
                        .password(vector.password())
                        .build()
        );
    }

    /**
     * Admin R2dbcEntityTemplate
     */
    @Bean
    public R2dbcEntityTemplate adminR2dbcEntityTemplate(ConnectionFactory adminConnectionFactory) {
        return new R2dbcEntityTemplate(adminConnectionFactory);
    }

    /**
     * Vector R2dbcEntityTemplate
     */
    @Bean
    public R2dbcEntityTemplate vectorR2dbcEntityTemplate(ConnectionFactory vectorConnectionFactory) {
        return new R2dbcEntityTemplate(vectorConnectionFactory);
    }
}
