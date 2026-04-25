package com.maghert.examcommon.exception;

public class NotNullException extends BusinessException {

    public NotNullException() {
        super(400, "参数不能为空");
    }

    public NotNullException(String message) {
        super(400, message);
    }
}
