package com.maghert.examcommon.apitest.generation;

import com.maghert.examcommon.apitest.collector.ApiEndpointMetadata;
import com.maghert.examcommon.apitest.model.ApiEndpointSpec;
import com.maghert.examcommon.apitest.model.ApiFieldSpec;
import com.maghert.examcommon.apitest.model.ApiTestSuite;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ApiTestSuiteBuilder {

    private final ConstraintIntrospector introspector;

    private final ApiScenarioPlanner scenarioPlanner;

    public ApiTestSuiteBuilder(ConstraintIntrospector introspector, ApiScenarioPlanner scenarioPlanner) {
        this.introspector = introspector;
        this.scenarioPlanner = scenarioPlanner;
    }

    public ApiTestSuite build(String serviceName, List<ApiEndpointMetadata> metadataList) {
        List<ApiEndpointSpec> endpoints = new ArrayList<>();
        for (ApiEndpointMetadata metadata : metadataList) {
            ApiEndpointSpec endpoint = new ApiEndpointSpec();
            endpoint.setEndpointKey(metadata.getHttpMethod() + " " + metadata.getPath());
            endpoint.setHandler(metadata.getHandler());
            endpoint.setHttpMethod(metadata.getHttpMethod());
            endpoint.setPath(metadata.getPath());
            endpoint.setValidationBound(metadata.isValidationBound());
            endpoint.setRequestBodyClass(metadata.getRequestBodyType() == null ? null : metadata.getRequestBodyType().getName());

            List<ApiFieldSpec> fields = introspector.describeFields(metadata.getRequestBodyType());
            endpoint.setFields(fields);
            endpoint.setScenarios(scenarioPlanner.plan(metadata, fields));
            endpoints.add(endpoint);
        }

        endpoints.sort(Comparator.comparing(ApiEndpointSpec::getPath).thenComparing(ApiEndpointSpec::getHttpMethod));
        ApiTestSuite suite = new ApiTestSuite();
        suite.setServiceName(serviceName);
        suite.setGeneratedAt(OffsetDateTime.now().toString());
        suite.setEndpoints(endpoints);
        return suite;
    }
}

