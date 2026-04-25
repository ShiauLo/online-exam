package com.maghert.examcommon.exception;

public class VerifyCodeNotEqualException extends VerifyCodeException {

    public VerifyCodeNotEqualException() {
        super("验证码错误，请稍后重试");
    }

    public VerifyCodeNotEqualException(String message) {
        super(message);
    }
}
