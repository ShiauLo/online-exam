package com.maghert.examissuecore.model.enums;

import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.exception.DomainErrorCode;

import java.util.Locale;

public enum IssueStatus {
    PENDING,
    PROCESSING,
    CLOSED;

    public static IssueStatus from(String raw) throws BusinessException {
        try {
            return IssueStatus.valueOf(raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(DomainErrorCode.ISSUE_STATUS_INVALID);
        }
    }
}
