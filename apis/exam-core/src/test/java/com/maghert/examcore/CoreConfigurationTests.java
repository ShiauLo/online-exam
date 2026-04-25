package com.maghert.examcore;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreConfigurationTests {

    @Test
    void shouldExposeExamCoreApplicationName() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application.yaml"));
        Properties properties = Objects.requireNonNull(factory.getObject());

        assertEquals("exam-core", properties.get("spring.application.name"));
        assertEquals("dev, local", properties.get("spring.profiles.active"));
    }

    @Test
    void shouldRegisterExamCoreModuleInRootPom() throws Exception {
        String rootPom = Files.readString(Path.of("..", "pom.xml"));
        assertTrue(rootPom.contains("<module>exam-core</module>"));
    }

    @Test
    void shouldProvideLocalProfileTemplate() throws Exception {
        String localProperties = Files.readString(Path.of("src", "main", "resources", "application-local.properties"));

        assertTrue(localProperties.contains("EXAM_NACOS_ADDR="));
        assertTrue(localProperties.contains("EXAM_DB_HOST="));
        assertTrue(localProperties.contains("EXAM_DB_NAME="));
        assertTrue(localProperties.contains("spring.cloud.nacos.discovery.fail-fast=false"));
    }
}
