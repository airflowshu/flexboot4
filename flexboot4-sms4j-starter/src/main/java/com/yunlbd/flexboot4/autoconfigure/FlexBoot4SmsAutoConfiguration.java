package com.yunlbd.flexboot4.autoconfigure;

import com.yunlbd.flexboot4.config.SmsSupplierConfigDataSource;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import(SmsSupplierConfigDataSource.class)
@ComponentScan(
        basePackages = {
                "com.yunlbd.flexboot4.controller.sms",
                "com.yunlbd.flexboot4.service.sms"
        },
        excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = SpringBootConfiguration.class)
)
public class FlexBoot4SmsAutoConfiguration {
}
