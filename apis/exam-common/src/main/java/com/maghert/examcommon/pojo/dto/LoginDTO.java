package com.maghert.examcommon.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.maghert.examcommon.validation.ValidLoginRequest;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ValidLoginRequest
public class LoginDTO {

    /** 登录类型（password_login/one_key_login） */
    @NotBlank(message = "登录类型不能为空")
    private String loginType;

    /** 账号（手机号/邮箱/用户名，密码登录必填） */
    private String account;

    /** 密码（密码登录必填） */
    private String password;

    /** 手机号（一键登录必填） */
    private String phone;

    /** 验证码（可选，一键登录可加验证码兜底） */
    private String verifyCode;

}
