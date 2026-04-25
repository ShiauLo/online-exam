package com.maghert.examcommon.exception;

import com.maghert.examcommon.web.ApiResponseCode;
import lombok.Getter;

@Getter
public class BusinessException extends Exception {

    private final Integer code;
    private final Integer errorCode;

    public BusinessException() {
        super(ApiResponseCode.BAD_REQUEST.getDefaultMessage());
        this.code = ApiResponseCode.BAD_REQUEST.getCode();
        this.errorCode = null;
    }

    public BusinessException(int code, String message) {
        this(code, message, null);
    }

    public BusinessException(int code, String message, Integer errorCode) {
        super(message);
        this.code = code;
        this.errorCode = errorCode;
    }

    public BusinessException(DomainErrorCode errorCode) {
        this(errorCode.getResponseCode().getCode(), errorCode.getDefaultMessage(), errorCode.getCode());
    }
}
