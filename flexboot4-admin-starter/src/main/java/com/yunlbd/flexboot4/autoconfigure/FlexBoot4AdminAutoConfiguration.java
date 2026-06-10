package com.yunlbd.flexboot4.autoconfigure;

import com.yunlbd.flexboot4.common.GlobalExceptionHandler;
import com.yunlbd.flexboot4.config.CacheConfig;
import com.yunlbd.flexboot4.config.DictConfiguration;
import com.yunlbd.flexboot4.config.DistributedLockConfig;
import com.yunlbd.flexboot4.config.FileStorageConfig;
import com.yunlbd.flexboot4.config.IgnoreUrlsConfig;
import com.yunlbd.flexboot4.config.JacksonConfig;
import com.yunlbd.flexboot4.config.MailProperties;
import com.yunlbd.flexboot4.config.MinioConfig;
import com.yunlbd.flexboot4.config.MyBatisFlexConfiguration;
import com.yunlbd.flexboot4.config.OAuthClientConfig;
import com.yunlbd.flexboot4.config.OpenApiConfig;
import com.yunlbd.flexboot4.config.OpenApiTagGroupScanner;
import com.yunlbd.flexboot4.config.OperLogStreamConfig;
import com.yunlbd.flexboot4.config.PasswordEncoderConfig;
import com.yunlbd.flexboot4.listener.SysOperLogListener;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({
        CacheConfig.class,
        DictConfiguration.class,
        DistributedLockConfig.class,
        FileStorageConfig.class,
        GlobalExceptionHandler.class,
        IgnoreUrlsConfig.class,
        JacksonConfig.class,
        MailProperties.class,
        MinioConfig.class,
        MyBatisFlexConfiguration.class,
        OAuthClientConfig.class,
        OpenApiConfig.class,
        OpenApiTagGroupScanner.class,
        OperLogStreamConfig.class,
        PasswordEncoderConfig.class,
        SysOperLogListener.class
})
@ComponentScan(
        basePackages = {
                "com.yunlbd.flexboot4.cache",
                "com.yunlbd.flexboot4.common.aspect",
                "com.yunlbd.flexboot4.controller.ops",
                "com.yunlbd.flexboot4.controller.sys",
                "com.yunlbd.flexboot4.oauth",
                "com.yunlbd.flexboot4.security",
                "com.yunlbd.flexboot4.service.ops",
                "com.yunlbd.flexboot4.service.sys",
                "com.yunlbd.flexboot4.storage",
                "com.yunlbd.flexboot4.task",
                "com.yunlbd.flexboot4.util"
        },
        excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = SpringBootConfiguration.class)
)
public class FlexBoot4AdminAutoConfiguration {
}
