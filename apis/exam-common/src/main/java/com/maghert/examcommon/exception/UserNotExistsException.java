package com.maghert.examcommon.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserNotExistsException extends BusinessException {
    public UserNotExistsException() {
        super(404, "用户不存在");
    }

    public UserNotExistsException(int code, String message) {
        super(code, message);
    }
}
