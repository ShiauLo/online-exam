package com.maghert.examcommon.exception;

public class PasswordNotEqualException extends BusinessException {

    public PasswordNotEqualException() {
        super(401, "密码错误");
    }

    public PasswordNotEqualException(String message) {
        super(401, message);
    }
}
