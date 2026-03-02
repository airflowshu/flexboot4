package com.yunlbd.flexboot4;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * <p>Bootstrap 启动类：内部开发测试用，聚合所有 starter 模块</p>
 * <p>
 * 包含：Admin Starter + KB Starter + Media Starter
 * </p>
 * <p>
 * 外部项目应该创建自己的主应用类，并根据需要引入相应的 starter 依赖
 * </p>
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
@MapperScan("com.yunlbd.flexboot4.mapper")
public class BootstrapApplication {

    public static void main(String[] args) {
        SpringApplication.run(BootstrapApplication.class, args);
    }

}
