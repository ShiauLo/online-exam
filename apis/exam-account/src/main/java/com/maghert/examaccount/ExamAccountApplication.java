package com.maghert.examaccount;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.maghert.examaccount.mapper")
@ComponentScan(basePackages = {"com.maghert.examaccount", "com.maghert.examcommon.*"}) // 包含两个包
public class ExamAccountApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExamAccountApplication.class, args);
	}

}
