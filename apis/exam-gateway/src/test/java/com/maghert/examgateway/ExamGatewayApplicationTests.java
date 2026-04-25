package com.maghert.examgateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration," +
                "org.springframework.cloud.gateway.config.GatewayClassPathWarningAutoConfiguration",
        "spring.cloud.nacos.discovery.enabled=false"
})
class ExamGatewayApplicationTests {

    @Test
    void contextLoads() {
    }

}
