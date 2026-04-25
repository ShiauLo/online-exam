package com.maghert.examcommon.apitest.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ApiScenarioSpec {

    private String id;

    private String name;

    private String kind;

    private Map<String, Object> requestBody = new LinkedHashMap<>();

    private Integer expectedStatus;

    private boolean contractExecutable;

    private boolean integrationExecutable;

    private List<String> tags = new ArrayList<>();

    private String notes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Map<String, Object> getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(Map<String, Object> requestBody) {
        this.requestBody = requestBody;
    }

    public Integer getExpectedStatus() {
        return expectedStatus;
    }

    public void setExpectedStatus(Integer expectedStatus) {
        this.expectedStatus = expectedStatus;
    }

    public boolean isContractExecutable() {
        return contractExecutable;
    }

    public void setContractExecutable(boolean contractExecutable) {
        this.contractExecutable = contractExecutable;
    }

    public boolean isIntegrationExecutable() {
        return integrationExecutable;
    }

    public void setIntegrationExecutable(boolean integrationExecutable) {
        this.integrationExecutable = integrationExecutable;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

