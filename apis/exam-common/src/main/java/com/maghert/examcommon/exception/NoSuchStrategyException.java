package com.maghert.examcommon.exception;

public class NoSuchStrategyException extends BusinessException {

    public NoSuchStrategyException() {
        super(400, "不支持的策略类型");
    }

    public NoSuchStrategyException(String message) {
        super(400, message);
    }
}
