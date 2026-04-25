package com.maghert.examquestion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionConfigurationTests {

    @Test
    void shouldExposeExamQuestionApplicationName() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application.yaml"));
        Properties properties = Objects.requireNonNull(factory.getObject());

        assertEquals("exam-question", properties.get("spring.application.name"));
        assertEquals("${EXAM_RESOURCE_STORAGE_ROOT:./tmp_resource}", properties.get("exam.resource.local-storage-root"));
    }

    @Test
    void shouldRegisterExamQuestionModuleInRootPom() throws Exception {
        String rootPom = Files.readString(Path.of("..", "pom.xml"));
        assertTrue(rootPom.contains("<module>exam-question</module>"));
    }

    @Test
    void shouldProvideLocalProfileTemplate() throws Exception {
        String localProperties = Files.readString(Path.of("src", "main", "resources", "application-local.properties"));

        assertTrue(localProperties.contains("EXAM_NACOS_ADDR="));
        assertTrue(localProperties.contains("EXAM_DB_HOST="));
        assertTrue(localProperties.contains("EXAM_DB_NAME="));
        assertTrue(localProperties.contains("EXAM_RESOURCE_STORAGE_ROOT="));
    }
}
