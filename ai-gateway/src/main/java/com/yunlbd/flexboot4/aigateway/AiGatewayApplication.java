package com.yunlbd.flexboot4.aigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AiGatewayApplication {
    static void main(String[] args) {
        SpringApplication.run(AiGatewayApplication.class, args);
    }
}
