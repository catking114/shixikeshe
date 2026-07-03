package com.example.keshe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.example.keshe"})
public class KesheApplication {
    public static void main(String[] args) {
        SpringApplication.run(KesheApplication.class, args);
    }
}
