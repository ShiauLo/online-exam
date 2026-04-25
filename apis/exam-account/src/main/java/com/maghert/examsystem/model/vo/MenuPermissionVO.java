package com.maghert.examsystem.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MenuPermissionVO {
    private String menuId;
    private String menuName;
    private String path;
    private String icon;
}
