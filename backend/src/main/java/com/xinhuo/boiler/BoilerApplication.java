package com.xinhuo.boiler;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan("com.xinhuo.boiler.mapper")
@SpringBootApplication
public class BoilerApplication {
    public static void main(String[] args) {
        SpringApplication.run(BoilerApplication.class, args);
    }
}
