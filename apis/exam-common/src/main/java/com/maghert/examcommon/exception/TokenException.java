package com.maghert.examcommon.exception;

public class TokenException extends BusinessException {
    public TokenException() {
        super(401, "未登录或登录态失效");
    }

    public TokenException(String message) {
        super(401, message);
    }
}
