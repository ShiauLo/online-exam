package com.maghert.examclass;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.maghert.examclass.mapper")
@ComponentScan(basePackages = {"com.maghert.examclass", "com.maghert.examcommon"})
public class ExamClassApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamClassApplication.class, args);
    }
}
