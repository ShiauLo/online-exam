package com.maghert.examgateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayConfigurationTests {

    @Test
    void shouldRouteNestedAccountPathsThroughGateway() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application.yaml"));
        Properties properties = Objects.requireNonNull(factory.getObject());

        assertEquals("Path=/api/account/**",
                properties.get("spring.cloud.gateway.server.webflux.routes[0].predicates[0]"));
        assertEquals("Path=/api/class/**",
                properties.get("spring.cloud.gateway.server.webflux.routes[1].predicates[0]"));
        assertEquals("Path=/api/system/**",
                properties.get("spring.cloud.gateway.server.webflux.routes[2].predicates[0]"));
        assertEquals("Path=/api/question/**",
                properties.get("spring.cloud.gateway.server.webflux.routes[3].predicates[0]"));
        assertEquals("Path=/api/paper/**",
                properties.get("spring.cloud.gateway.server.webflux.routes[4].predicates[0]"));
        assertEquals("Path=/api/exam/core/**",
                properties.get("spring.cloud.gateway.server.webflux.routes[5].predicates[0]"));
        assertEquals("Path=/api/score/**",
                properties.get("spring.cloud.gateway.server.webflux.routes[6].predicates[0]"));
        assertEquals("Path=/api/resource/**",
                properties.get("spring.cloud.gateway.server.webflux.routes[7].predicates[0]"));
        assertEquals("Path=/api/issue/core/**",
                properties.get("spring.cloud.gateway.server.webflux.routes[8].predicates[0]"));
        assertEquals("Path=/api/exam/realtime/**",
                properties.get("spring.cloud.gateway.server.webflux.routes[9].predicates[0]"));
        assertEquals("Path=/api/issue/notify/**",
                properties.get("spring.cloud.gateway.server.webflux.routes[10].predicates[0]"));
        assertEquals(Boolean.TRUE,
                properties.get("spring.cloud.gateway.server.webflux.globalcors.add-to-simple-url-handler-mapping"));
        assertEquals("DedupeResponseHeader=Access-Control-Allow-Origin Access-Control-Allow-Credentials, RETAIN_FIRST",
                properties.get("spring.cloud.gateway.server.webflux.default-filters[0]"));
        assertTrue(readApplicationYaml().contains("http://127.0.0.1:*"));
        assertTrue(readApplicationYaml().contains("http://localhost:*"));
        assertEquals("/api/account/send/**", properties.get("exam.auth.ignore-paths[3]"));
    }

    private String readApplicationYaml() {
        try {
            return new String(new ClassPathResource("application.yaml").getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("读取 application.yaml 失败", exception);
        }
    }
}
