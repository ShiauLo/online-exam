package com.maghert.examresource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceConfigurationTests {

    @Test
    void shouldExposeExamResourceApplicationName() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("exam-class-host.yaml"));
        Properties properties = Objects.requireNonNull(factory.getObject());

        assertEquals("exam-class", properties.get("spring.application.name"));
        assertEquals("dev, local", properties.get("spring.profiles.active"));
        assertEquals("8082", String.valueOf(properties.get("server.port")));
    }

    @Test
    void shouldRegisterExamResourceModuleInRootPom() throws Exception {
        String rootPom = Files.readString(Path.of("..", "pom.xml"));
        assertTrue(rootPom.contains("<module>exam-class</module>"));
    }

    @Test
    void shouldProvideLocalProfileTemplate() throws Exception {
        String localProperties = Files.readString(Path.of("src", "main", "resources", "exam-class-host-local.properties"));

        assertTrue(localProperties.contains("EXAM_NACOS_ADDR="));
        assertTrue(localProperties.contains("EXAM_RESOURCE_STORAGE_ROOT="));
    }
}
