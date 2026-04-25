package com.maghert.examsystem.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PermissionQueryVO {
    private String roleType;
    private List<MenuPermissionVO> menuList;
    private List<RoutePermissionVO> routeList;
    private List<String> buttonList;
}
