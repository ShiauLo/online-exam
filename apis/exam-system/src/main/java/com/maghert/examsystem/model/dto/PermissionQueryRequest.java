package com.maghert.examsystem.model.dto;

import lombok.Data;

@Data
public class PermissionQueryRequest {
    private String roleType;
    private Long accountId;
}
