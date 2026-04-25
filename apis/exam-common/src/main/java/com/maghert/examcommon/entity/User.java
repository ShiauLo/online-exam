package com.maghert.examcommon.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;
    private String username;
    private String phoneNumber;
    private Integer roleId;

    public void setUser(@NotNull User userThreadLocal) {

        this.id = userThreadLocal.getId();
        this.username = userThreadLocal.getUsername();
        this.phoneNumber = userThreadLocal.getPhoneNumber();
        this.roleId = userThreadLocal.getRoleId();

    }

}
