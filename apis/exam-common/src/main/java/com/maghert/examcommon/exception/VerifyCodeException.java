package com.maghert.examcommon.exception;

public class VerifyCodeException extends BusinessException{

    public VerifyCodeException(String message) {
        super(400,message);
    }
}
