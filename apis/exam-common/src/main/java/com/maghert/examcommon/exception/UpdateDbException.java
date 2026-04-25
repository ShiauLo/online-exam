package com.maghert.examcommon.exception;

public class UpdateDbException extends BusinessException {

    public UpdateDbException() {
        super(500, "数据库更新失败");
    }

    public UpdateDbException(String message) {
        super(500, message);
    }
}
