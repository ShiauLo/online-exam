package com.maghert.examcommon.exception;

public class LoginException extends BusinessException {

    public LoginException() {
        super(401, "登录失败，请联系管理员");
    }

    public LoginException(String message) {
        super(401, message);
    }
}
