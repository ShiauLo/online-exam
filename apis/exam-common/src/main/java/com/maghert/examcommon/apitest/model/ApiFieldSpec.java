package com.maghert.examcommon.apitest.model;

import java.util.ArrayList;
import java.util.List;

public class ApiFieldSpec {

    private String name;

    private String type;

    private List<String> semanticHints = new ArrayList<>();

    private List<ApiConstraintSpec> constraints = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getSemanticHints() {
        return semanticHints;
    }

    public void setSemanticHints(List<String> semanticHints) {
        this.semanticHints = semanticHints;
    }

    public List<ApiConstraintSpec> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<ApiConstraintSpec> constraints) {
        this.constraints = constraints;
    }

    public boolean hasConstraint(String type) {
        return constraints.stream().anyMatch(constraint -> type.equals(constraint.getType()));
    }

    public ApiConstraintSpec findConstraint(String type) {
        return constraints.stream()
                .filter(constraint -> type.equals(constraint.getType()))
                .findFirst()
                .orElse(null);
    }
}

