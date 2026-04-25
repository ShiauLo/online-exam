package com.maghert.examgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {ValidationAutoConfiguration.class})
@ComponentScan(basePackages = {"com.maghert.examgateway", "com.maghert.examcommon"})
public class ExamGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamGatewayApplication.class, args);
    }
}
