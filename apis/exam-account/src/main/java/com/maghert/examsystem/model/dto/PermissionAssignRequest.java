package com.maghert.examsystem.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PermissionAssignRequest {
    @NotNull(message = "accountId 不能为空")
    private Long accountId;

    @NotNull(message = "roleId 不能为空")
    private Integer roleId;

    private String expireTime;
}
