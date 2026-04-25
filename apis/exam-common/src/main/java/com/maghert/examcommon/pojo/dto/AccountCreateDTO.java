package com.maghert.examcommon.pojo.dto;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.experimental.Accessors;

@Data // Lombok注解：自动生成getter、setter、toString、equals、hashCode方法
@TableName("sys_user") // MyBatis-Plus注解：指定当前类对应的数据库表名
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountCreateDTO {

    private String username;

    private String password;

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
