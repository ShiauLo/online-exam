package com.maghert.examcommon.enums;

import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.exception.DomainErrorCode;

import java.util.Arrays;

public enum QuestionType {
    SINGLE("single"),
    MULTI("multi"),
    SUBJECTIVE("subjective");

    private final String code;

    QuestionType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static QuestionType fromCode(String code) throws BusinessException {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException(DomainErrorCode.QUESTION_TYPE_UNSUPPORTED));
    }
}
