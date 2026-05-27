package com.yunlbd.flexboot4.autoconfigure;

import com.yunlbd.flexboot4.config.CmsPageResourceConfig;
import com.yunlbd.flexboot4.config.CmsRenderProperties;
import com.yunlbd.flexboot4.config.CmsTemplateProperties;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({
        CmsPageResourceConfig.class,
        CmsRenderProperties.class,
        CmsTemplateProperties.class
})
@ComponentScan(
        basePackages = {
                "com.yunlbd.flexboot4.controller.cms",
                "com.yunlbd.flexboot4.service.cms"
        },
        excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = SpringBootConfiguration.class)
)
public class FlexBoot4CmsAutoConfiguration {
}
