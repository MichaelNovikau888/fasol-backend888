package com.fasol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FasolApplication {
    public static void main(String[] args) {
        SpringApplication.run(FasolApplication.class, args);
    }
}
