package com.maghert.examresource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.cloud.nacos.discovery.enabled=false"
}, classes = ExamResourceApplicationTests.TestApplication.class)
class ExamResourceApplicationTests {

    @Test
    void contextLoads() {
    }

    @SpringBootConfiguration
    static class TestApplication {
    }
}
