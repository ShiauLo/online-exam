package com.maghert.examcommon.apitest.model;

import java.util.ArrayList;
import java.util.List;

public class ApiEndpointSpec {

    private String endpointKey;

    private String handler;

    private String httpMethod;

    private String path;

    private String requestBodyClass;

    private boolean validationBound;

    private List<ApiFieldSpec> fields = new ArrayList<>();

    private List<ApiScenarioSpec> scenarios = new ArrayList<>();

    public String getEndpointKey() {
        return endpointKey;
    }

    public void setEndpointKey(String endpointKey) {
        this.endpointKey = endpointKey;
    }

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRequestBodyClass() {
        return requestBodyClass;
    }

    public void setRequestBodyClass(String requestBodyClass) {
        this.requestBodyClass = requestBodyClass;
    }

    public boolean isValidationBound() {
        return validationBound;
    }

    public void setValidationBound(boolean validationBound) {
        this.validationBound = validationBound;
    }

    public List<ApiFieldSpec> getFields() {
        return fields;
    }

    public void setFields(List<ApiFieldSpec> fields) {
        this.fields = fields;
    }

    public List<ApiScenarioSpec> getScenarios() {
        return scenarios;
    }

    public void setScenarios(List<ApiScenarioSpec> scenarios) {
        this.scenarios = scenarios;
    }
}

