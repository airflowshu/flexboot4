package com.yunlbd.flexboot4;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching
@EnableAsync
@MapperScan("com.yunlbd.flexboot4.mapper")
public class Flexboot4Application {

	public static void main(String[] args) {
		SpringApplication.run(Flexboot4Application.class, args);
	}

}
