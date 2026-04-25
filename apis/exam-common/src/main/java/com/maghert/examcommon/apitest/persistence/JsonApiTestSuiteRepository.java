package com.maghert.examcommon.apitest.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maghert.examcommon.apitest.model.ApiEndpointSpec;
import com.maghert.examcommon.apitest.model.ApiTestSuite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonApiTestSuiteRepository {

    private final ObjectMapper objectMapper;

    private final Path baseDir;

    public JsonApiTestSuiteRepository(ObjectMapper objectMapper, String baseDir) {
        this.objectMapper = objectMapper.copy().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.baseDir = Paths.get(baseDir);
    }

    public void writeGeneratedSuite(String serviceName, ApiTestSuite suite) throws IOException {
        Path output = generatedPath(serviceName);
        Files.createDirectories(output.getParent());
        ApiTestSuite existing = readIfExists(output);
        if (existing != null) {
            suite.setGeneratedAt(existing.getGeneratedAt());
            if (isSemanticallyEqual(existing, suite)) {
                return;
            }
        }
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), suite);
    }

    public ApiTestSuite loadMergedSuite(String serviceName) throws IOException {
        ApiTestSuite generated = readIfExists(generatedPath(serviceName));
        ApiTestSuite manual = readIfExists(manualPath(serviceName));
        if (generated == null) {
            return manual;
        }
        if (manual == null) {
            return generated;
        }
        Map<String, ApiEndpointSpec> manualEndpoints = new LinkedHashMap<>();
        for (ApiEndpointSpec endpoint : manual.getEndpoints()) {
            manualEndpoints.put(endpoint.getEndpointKey(), endpoint);
        }
        for (ApiEndpointSpec endpoint : generated.getEndpoints()) {
            ApiEndpointSpec manualEndpoint = manualEndpoints.get(endpoint.getEndpointKey());
            if (manualEndpoint != null && manualEndpoint.getScenarios() != null) {
                Map<String, Boolean> existingScenarioIds = new LinkedHashMap<>();
                endpoint.getScenarios().forEach(scenario -> existingScenarioIds.put(scenario.getId(), Boolean.TRUE));
                manualEndpoint.getScenarios().stream()
                        .filter(scenario -> !existingScenarioIds.containsKey(scenario.getId()))
                        .forEach(endpoint.getScenarios()::add);
            }
        }
        return generated;
    }

    public Path generatedPath(String serviceName) {
        return baseDir.resolve(serviceName).resolve("generated-suite.json");
    }

    public Path manualPath(String serviceName) {
        return baseDir.resolve(serviceName).resolve("manual-suite.json");
    }

    private ApiTestSuite readIfExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }
        return objectMapper.readValue(path.toFile(), ApiTestSuite.class);
    }

    private boolean isSemanticallyEqual(ApiTestSuite left, ApiTestSuite right) {
        JsonNode leftNode = objectMapper.valueToTree(left).deepCopy();
        JsonNode rightNode = objectMapper.valueToTree(right).deepCopy();
        leftNode = stripGeneratedAt(leftNode);
        rightNode = stripGeneratedAt(rightNode);
        return leftNode.equals(rightNode);
    }

    private JsonNode stripGeneratedAt(JsonNode node) {
        if (node.isObject()) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) node).remove("generatedAt");
        }
        return node;
    }
}

