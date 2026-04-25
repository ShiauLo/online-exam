package com.maghert.examcommon.exception;

public class UpdateMYSQLException extends BusinessException {
    public UpdateMYSQLException() {
        super(500, "数据库更新失败");
    }

    public UpdateMYSQLException(String message) {
        super(500, message);
    }
}
