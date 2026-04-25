package com.maghert.examcore;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration",
        "spring.cloud.nacos.discovery.enabled=false"
}, classes = ExamCoreApplicationTests.TestApplication.class)
class ExamCoreApplicationTests {

    @Test
    void contextLoads() {
    }

    @SpringBootConfiguration
    static class TestApplication {
    }
}
