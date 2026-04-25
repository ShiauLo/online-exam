package com.maghert.examcommon.exception;

public class AccountCreateException extends BusinessException {

    public AccountCreateException() {
        super(500, "创建账户失败");
    }

    public AccountCreateException(int code, String message) {
        super(code, message);
    }
}
