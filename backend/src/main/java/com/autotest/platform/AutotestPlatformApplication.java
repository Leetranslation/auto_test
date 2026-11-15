package com.autotest.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.autotest.platform.mapper")
@EnableScheduling
public class AutotestPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutotestPlatformApplication.class, args);
    }
}
