package com.maghert.examcommon.enums;

import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.exception.DomainErrorCode;

import java.util.Arrays;

public enum QuestionAuditStatus {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected");

    private final String code;

    QuestionAuditStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static QuestionAuditStatus fromCode(String code) throws BusinessException {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException(DomainErrorCode.QUESTION_AUDIT_STATUS_UNSUPPORTED));
    }
}
