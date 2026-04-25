package com.maghert.examclass;

import com.maghert.exampaper.ExamPaperApplication;
import com.maghert.exampaper.config.IdGeneratorConfig;
import com.maghert.exampaper.handler.GlobalExceptionHandler;
import com.maghert.examquestion.ExamQuestionApplication;
import com.maghert.examresource.ExamResourceApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

@SpringBootApplication
@MapperScan(
        basePackages = {
                "com.maghert.examclass.mapper",
                "com.maghert.examquestion.mapper",
                "com.maghert.exampaper.mapper"
        },
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
)
@ComponentScan(
        basePackages = {
                "com.maghert.examclass",
                "com.maghert.examquestion",
                "com.maghert.exampaper",
                "com.maghert.examresource",
                "com.maghert.examcommon"
        },
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ExamQuestionApplication.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.maghert.examquestion.handler.GlobalExceptionHandler.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.maghert.examquestion.config.IdGeneratorConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.maghert.examquestion.MyMetaObjectHandler.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ExamPaperApplication.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = GlobalExceptionHandler.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = IdGeneratorConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.maghert.exampaper.MyMetaObjectHandler.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ExamResourceApplication.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.maghert.examresource.handler.GlobalExceptionHandler.class)
        }
)
public class ExamClassApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamClassApplication.class, args);
    }
}
