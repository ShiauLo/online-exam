package com.maghert.examcore;

import com.maghert.examissuecore.ExamIssueCoreApplication;
import com.maghert.examscore.ExamScoreApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

@SpringBootApplication
@MapperScan(
        basePackages = {
                "com.maghert.examcore.mapper",
                "com.maghert.examscore.mapper",
                "com.maghert.examissuecore.mapper"
        },
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
)
@ComponentScan(
        basePackages = {
                "com.maghert.examcore",
                "com.maghert.examscore",
                "com.maghert.examissuecore",
                "com.maghert.examcommon"
        },
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ExamScoreApplication.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.maghert.examscore.handler.GlobalExceptionHandler.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.maghert.examscore.config.IdGeneratorConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.maghert.examscore.MyMetaObjectHandler.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ExamIssueCoreApplication.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.maghert.examissuecore.handler.GlobalExceptionHandler.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.maghert.examissuecore.config.IdGeneratorConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.maghert.examissuecore.MyMetaObjectHandler.class)
        }
)
public class ExamCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamCoreApplication.class, args);
    }
}
