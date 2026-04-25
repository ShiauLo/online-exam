package com.maghert.examcommon.apitest.collector;

public class ApiEndpointMetadata {

    private String handler;

    private String httpMethod;

    private String path;

    private Class<?> requestBodyType;

    private boolean validationBound;

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

    public Class<?> getRequestBodyType() {
        return requestBodyType;
    }

    public void setRequestBodyType(Class<?> requestBodyType) {
        this.requestBodyType = requestBodyType;
    }

    public boolean isValidationBound() {
        return validationBound;
    }

    public void setValidationBound(boolean validationBound) {
        this.validationBound = validationBound;
    }
}

