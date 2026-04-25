package com.maghert.examcommon.apitest.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class ApiConstraintSpec {

    private String type;

    private Map<String, Object> attributes = new LinkedHashMap<>();

    public ApiConstraintSpec() {
    }

    public ApiConstraintSpec(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Object attribute(String key) {
        return attributes.get(key);
    }
}

