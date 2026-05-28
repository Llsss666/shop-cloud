package com.example.freshshop;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

// 完全复原！
@SpringBootApplication
@MapperScan("com.example.freshshop.mapper")
@EnableFeignClients
public class MarketingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MarketingServiceApplication.class, args);
    }
}