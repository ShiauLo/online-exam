package com.maghert.examcommon.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountQueryVO {
    private Long accountId;
    private String username;
    private String realName;
    private String roleType;
    private String phone;
    private String email;
    private String status;
}
