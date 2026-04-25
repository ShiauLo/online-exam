package com.maghert.exampaper;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.maghert.exampaper.mapper")
@ComponentScan(basePackages = {"com.maghert.exampaper", "com.maghert.examcommon"})
public class ExamPaperApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamPaperApplication.class, args);
    }
}
