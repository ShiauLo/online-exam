package com.maghert.examcommon.pojo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountUpdateDTO {

    @NotNull
    @JsonAlias("accountId")
    private Long userId;

    private String realName;

    @Min(3)
    @Max(6)
    private Integer roleId;

    private String roleType;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$",
            message = "请输入有效的11位手机号"
    )
    @JsonAlias("phone")
    private String phoneNumber;

    @Email(message = "请输入有效的邮箱地址")
    private String email;

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = normalize(phoneNumber);
    }

    public void setEmail(String email) {
        this.email = normalize(email);
    }

    public void setRoleType(String roleType) {
        this.roleType = normalize(roleType);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

}
