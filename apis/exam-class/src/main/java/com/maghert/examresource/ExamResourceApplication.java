package com.maghert.examresource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.maghert.examresource", "com.maghert.examcommon"})
public class ExamResourceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamResourceApplication.class, args);
    }
}
