package com.maghert.examcommon.apitest.model;

import java.util.ArrayList;
import java.util.List;

public class ApiTestSuite {

    private String serviceName;

    private String generatedAt;

    private List<ApiEndpointSpec> endpoints = new ArrayList<>();

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public List<ApiEndpointSpec> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<ApiEndpointSpec> endpoints) {
        this.endpoints = endpoints;
    }
}

