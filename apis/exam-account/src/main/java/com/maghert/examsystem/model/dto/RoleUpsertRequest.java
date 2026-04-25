package com.maghert.examsystem.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class RoleUpsertRequest {
    private Long roleId;

    @NotBlank(message = "roleName 不能为空")
    private String roleName;

    @NotEmpty(message = "permissionIds 不能为空")
    private List<String> permissionIds;

    private Long templateId;
}
