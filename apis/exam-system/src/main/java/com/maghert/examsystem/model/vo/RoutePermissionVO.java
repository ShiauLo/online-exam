package com.maghert.examsystem.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoutePermissionVO {
    private String path;
    private String component;
}
