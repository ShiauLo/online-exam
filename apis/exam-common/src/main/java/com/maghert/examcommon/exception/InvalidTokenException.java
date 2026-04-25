package com.maghert.examcommon.exception;

public class InvalidTokenException extends TokenException {

    public InvalidTokenException() {
        super("Token 无效");
    }

    public InvalidTokenException(String message) {
        super(message);
    }
}
