package com.maghert.examaccount.enums;

import com.maghert.examcommon.exception.LoginException;
import lombok.Getter;

import static com.maghert.examaccount.constants.AccountConstants.*;

/**
 * 登录类型枚举
 */
public enum LoginTypeEnum {
    /** 密码登录（手机号/邮箱/用户名+密码） */
    PASSWORD_LOGIN(CODE_PASSWORD_LOGIN),
    /** 手机号一键登录 */
    ONE_KEY_LOGIN(CODE_ONE_KEY_LOGIN);

    // getter
    @Getter
    private final String code;

    LoginTypeEnum(String code) {
        this.code = code;
    }

    // 根据code获取枚举（工厂模式用）
    public static LoginTypeEnum getByCode(String code) throws LoginException {
        for (LoginTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new LoginException("不支持的登录类型：" + code);
    }

}
