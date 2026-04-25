package com.maghert.examcore;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.maghert.examcore.mapper")
@ComponentScan(basePackages = {"com.maghert.examcore", "com.maghert.examcommon"})
public class ExamCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamCoreApplication.class, args);
    }
}
