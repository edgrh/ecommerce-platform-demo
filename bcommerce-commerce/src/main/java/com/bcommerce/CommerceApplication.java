package com.bcommerce;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableRabbit
@MapperScan("com.bcommerce.mapper")
public class CommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommerceApplication.class, args);
    }
}
