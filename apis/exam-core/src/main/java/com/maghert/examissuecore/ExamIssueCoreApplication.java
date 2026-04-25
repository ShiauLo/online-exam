package com.maghert.examissuecore;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.maghert.examissuecore.mapper")
public class ExamIssueCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamIssueCoreApplication.class, args);
    }
}
