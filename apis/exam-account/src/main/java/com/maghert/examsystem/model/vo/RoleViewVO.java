package com.maghert.examsystem.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RoleViewVO {
    private Long roleId;
    private String roleName;
    private List<String> permissionIds;
    private Long templateId;
}
