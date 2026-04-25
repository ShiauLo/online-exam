package com.maghert.examcommon.apitest.generation;

import com.maghert.examcommon.apitest.collector.ApiEndpointMetadata;
import com.maghert.examcommon.apitest.model.ApiConstraintSpec;
import com.maghert.examcommon.apitest.model.ApiFieldSpec;
import com.maghert.examcommon.apitest.model.ApiScenarioSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ApiScenarioPlanner {

    private final SampleValueResolverRegistry valueResolvers;

    public ApiScenarioPlanner(SampleValueResolverRegistry valueResolvers) {
        this.valueResolvers = valueResolvers;
    }

    public List<ApiScenarioSpec> plan(ApiEndpointMetadata metadata, List<ApiFieldSpec> fields) {
        List<ApiScenarioSpec> scenarios = new ArrayList<>();
        Map<String, Object> validPayload = buildValidPayload(fields);
        scenarios.add(newScenario(
                metadata,
                "happy_path",
                "happy-path",
                validPayload,
                200,
                true,
                true,
                List.of("smoke", "contract"),
                null));

        for (ApiFieldSpec field : fields) {
            for (ApiConstraintSpec constraint : field.getConstraints()) {
                scenarios.addAll(buildConstraintScenarios(metadata, field, constraint, validPayload));
            }
        }
        scenarios.addAll(buildBehaviorScenarios(metadata, fields, validPayload));
        return scenarios;
    }

    private List<ApiScenarioSpec> buildBehaviorScenarios(
            ApiEndpointMetadata metadata,
            List<ApiFieldSpec> fields,
            Map<String, Object> validPayload) {
        List<ApiScenarioSpec> scenarios = new ArrayList<>();
        boolean hasIdentityField = fields.stream().anyMatch(this::isIdentityField);
        String path = metadata.getPath();

        if (hasIdentityField && isStateMutation(metadata)) {
            scenarios.add(nonExecutableScenario(
                    metadata,
                    "resource_not_found",
                    validPayload,
                    404,
                    List.of("whitebox", "resource"),
                    "Requires a mocked or seeded service state where the target resource does not exist."));
            scenarios.add(nonExecutableScenario(
                    metadata,
                    "backend_failure",
                    validPayload,
                    500,
                    List.of("whitebox", "persistence"),
                    "Requires a mocked repository/service failure path."));
        }

        if (path.contains("/login")) {
            Map<String, Object> payload = new LinkedHashMap<>(validPayload);
            payload.put("loginType", "unsupported_login_type");
            scenarios.add(newScenario(
                    metadata,
                    "unsupported_login_type",
                    "business-rule",
                    payload,
                    metadata.isValidationBound() ? 400 : null,
                    metadata.isValidationBound(),
                    true,
                    List.of("auth", metadata.isValidationBound() ? "contract" : "integration"),
                    "Covers invalid login strategy selection."));
        }

        if (path.contains("/logout")) {
            scenarios.add(nonExecutableScenario(
                    metadata,
                    "cache_delete_failure",
                    validPayload,
                    500,
                    List.of("whitebox", "cache"),
                    "Requires Redis delete failure simulation."));
        }
        return scenarios;
    }

    private List<ApiScenarioSpec> buildConstraintScenarios(
            ApiEndpointMetadata metadata,
            ApiFieldSpec field,
            ApiConstraintSpec constraint,
            Map<String, Object> validPayload) {
        List<ApiScenarioSpec> scenarios = new ArrayList<>();
        if ("NOT_NULL".equals(constraint.getType())) {
            scenarios.add(invalidScenario(metadata, field, "missing", validPayload, null, "required-field"));
        } else if ("NOT_BLANK".equals(constraint.getType())) {
            scenarios.add(invalidScenario(metadata, field, "blank", validPayload, "", "required-field"));
        } else if ("EMAIL".equals(constraint.getType()) || "PATTERN".equals(constraint.getType())) {
            scenarios.add(invalidScenario(metadata, field, "invalid_format", validPayload,
                    valueResolvers.resolveInvalidValue(field, constraint), "format"));
        } else if ("MIN".equals(constraint.getType()) || "MAX".equals(constraint.getType())) {
            scenarios.add(invalidScenario(metadata, field, "boundary", validPayload,
                    valueResolvers.resolveInvalidValue(field, constraint), "boundary"));
        }
        return scenarios;
    }

    private ApiScenarioSpec invalidScenario(
            ApiEndpointMetadata metadata,
            ApiFieldSpec field,
            String suffix,
            Map<String, Object> validPayload,
            Object invalidValue,
            String tag) {
        Map<String, Object> payload = new LinkedHashMap<>(validPayload);
        if (invalidValue == null) {
            payload.remove(field.getName());
        } else {
            payload.put(field.getName(), invalidValue);
        }
        boolean validationBound = metadata.isValidationBound();
        return newScenario(
                metadata,
                field.getName() + "_" + suffix,
                tag,
                payload,
                validationBound ? 400 : null,
                validationBound,
                true,
                List.of(tag, validationBound ? "contract" : "integration"),
                validationBound ? null : "DTO declares constraints, but controller request body is not bound with @Valid/@Validated.");
    }

    private ApiScenarioSpec newScenario(
            ApiEndpointMetadata metadata,
            String idSuffix,
            String kind,
            Map<String, Object> payload,
            Integer expectedStatus,
            boolean contractExecutable,
            boolean integrationExecutable,
            List<String> tags,
            String notes) {
        ApiScenarioSpec scenario = new ApiScenarioSpec();
        scenario.setId(slug(metadata.getHttpMethod() + "-" + metadata.getPath()) + "-" + idSuffix);
        scenario.setName(idSuffix);
        scenario.setKind(kind);
        scenario.setRequestBody(payload);
        scenario.setExpectedStatus(expectedStatus);
        scenario.setContractExecutable(contractExecutable);
        scenario.setIntegrationExecutable(integrationExecutable);
        scenario.setTags(new ArrayList<>(tags));
        scenario.setNotes(notes);
        return scenario;
    }

    private ApiScenarioSpec nonExecutableScenario(
            ApiEndpointMetadata metadata,
            String idSuffix,
            Map<String, Object> payload,
            Integer expectedStatus,
            List<String> tags,
            String notes) {
        return newScenario(
                metadata,
                idSuffix,
                "whitebox",
                new LinkedHashMap<>(payload),
                expectedStatus,
                false,
                false,
                tags,
                notes);
    }

    private Map<String, Object> buildValidPayload(List<ApiFieldSpec> fields) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (ApiFieldSpec field : fields) {
            payload.put(field.getName(), valueResolvers.resolveValidValue(field));
        }
        return payload;
    }

    private String slug(String value) {
        return value.replaceAll("[^a-zA-Z0-9]+", "-").replaceAll("(^-|-$)", "").toLowerCase();
    }

    private boolean isIdentityField(ApiFieldSpec field) {
        return "id".equals(field.getName()) || field.getName().endsWith("Id") || field.getName().endsWith("id");
    }

    private boolean isStateMutation(ApiEndpointMetadata metadata) {
        return "PUT".equals(metadata.getHttpMethod())
                || "DELETE".equals(metadata.getHttpMethod())
                || metadata.getPath().contains("/freeze")
                || metadata.getPath().contains("/reset-password")
                || metadata.getPath().contains("/update")
                || metadata.getPath().contains("/delete");
    }
}

