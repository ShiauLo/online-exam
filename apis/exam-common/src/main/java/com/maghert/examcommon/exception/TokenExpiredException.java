package com.maghert.examcommon.exception;

public class TokenExpiredException extends TokenException {

    public TokenExpiredException() {
        super("Token 已过期");
    }

    public TokenExpiredException(String message) {
        super(message);
    }
}
