package com.maghert.examcommon.exception;

public class UpdateRedisException extends UpdateDbException {

    public UpdateRedisException() {
        super("Redis 更新失败");
    }

    public UpdateRedisException(String message) {
        super(message);
    }
}
