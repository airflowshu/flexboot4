package com.yunlbd.flexboot4.config;

import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.spring.boot.MyBatisFlexCustomizer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisFlexConfig implements MyBatisFlexCustomizer {

    @Override
    public void customize(FlexGlobalConfig globalConfig) {
        // Configure logic delete, audit fields etc if needed
        globalConfig.setPrintBanner(false);
    }
}
