package com.maghert.examquestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.maghert.examquestion", "com.maghert.examcommon"})
public class ExamQuestionApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamQuestionApplication.class, args);
    }
}
