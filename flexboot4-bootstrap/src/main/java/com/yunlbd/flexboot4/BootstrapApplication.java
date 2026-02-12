package com.yunlbd.flexboot4;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * <p>启动类：聚合 Admin + KB 模块</p>
 * <p>
 * 由于 ComponentScan 默认扫描当前包及其子包，
 * 此类位于 com.yunlbd.flexboot4，因此会自动扫描到：
 * - com.yunlbd.flexboot4 (admin 模块)
 * - com.yunlbd.flexboot4.service.kb (kb 模块)
 * </p>
 * 增加了排除扫描规则，避免把 Flexboot4Application 当成配置类扫描进来：
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
@ComponentScan(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = Flexboot4Application.class
        )
)
@MapperScan("com.yunlbd.flexboot4.mapper")
public class BootstrapApplication {

    static void main(String[] args) {
        SpringApplication.run(BootstrapApplication.class, args);
    }

}
