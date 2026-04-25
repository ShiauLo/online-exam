package com.maghert.examscore;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration",
        "spring.cloud.nacos.discovery.enabled=false"
}, classes = ExamScoreApplicationTests.TestApplication.class)
class ExamScoreApplicationTests {

    @Test
    void contextLoads() {
    }

    @SpringBootConfiguration
    static class TestApplication {
    }
}
