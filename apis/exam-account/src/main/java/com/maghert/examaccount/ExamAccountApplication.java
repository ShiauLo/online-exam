package com.maghert.examaccount;

import com.maghert.examsystem.ExamSystemApplication;
import com.maghert.examsystem.config.IdGeneratorConfig;
import com.maghert.examsystem.handler.GlobalExceptionHandler;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

@SpringBootApplication
@MapperScan(
        basePackages = {
                "com.maghert.examaccount.mapper",
                "com.maghert.examsystem.mapper"
        },
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
)
@ComponentScan(
        basePackages = {
                "com.maghert.examaccount",
                "com.maghert.examsystem",
                "com.maghert.examcommon"
        },
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ExamSystemApplication.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = GlobalExceptionHandler.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = IdGeneratorConfig.class)
        }
)
public class ExamAccountApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExamAccountApplication.class, args);
	}

}
