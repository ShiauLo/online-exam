package com.maghert.examscore;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.maghert.examscore.mapper")
@ComponentScan(basePackages = {"com.maghert.examscore", "com.maghert.examcommon"})
public class ExamScoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamScoreApplication.class, args);
    }
}
