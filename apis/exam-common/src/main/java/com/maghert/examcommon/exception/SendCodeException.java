package com.maghert.examcommon.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendCodeException extends BusinessException {
    public SendCodeException() {
        super();
    }
    public SendCodeException(int code,String message) {
        super(code,message);
    }
}
