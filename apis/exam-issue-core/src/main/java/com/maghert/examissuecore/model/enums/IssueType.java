package com.maghert.examissuecore.model.enums;

import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.exception.DomainErrorCode;

import java.util.Locale;

public enum IssueType {
    BUSINESS,
    EXAM,
    SYSTEM;

    public static IssueType from(String raw) throws BusinessException {
        try {
            return IssueType.valueOf(raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(DomainErrorCode.ISSUE_TYPE_INVALID);
        }
    }
}
