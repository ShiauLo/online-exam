package com.maghert.examcommon.exception;

import com.maghert.examcommon.web.ApiResponseCode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainErrorCodeTests {

    @Test
    void shouldMapDomainErrorsToStandardResponseCodes() {
        BusinessException classException = new BusinessException(DomainErrorCode.CLASS_NOT_FOUND);
        BusinessException systemException = new BusinessException(DomainErrorCode.PERMISSION_ASSIGNMENT_FORBIDDEN);

        assertEquals(1101, classException.getErrorCode());
        assertEquals(ApiResponseCode.NOT_FOUND.getCode(), classException.getCode());
        assertEquals(DomainErrorCode.CLASS_NOT_FOUND.getDefaultMessage(), classException.getMessage());

        assertEquals(1203, systemException.getErrorCode());
        assertEquals(ApiResponseCode.FORBIDDEN.getCode(), systemException.getCode());
        assertEquals(DomainErrorCode.PERMISSION_ASSIGNMENT_FORBIDDEN.getDefaultMessage(), systemException.getMessage());
    }

    @Test
    void shouldKeepDomainErrorCodesUnique() {
        Set<Integer> uniqueCodes = Arrays.stream(DomainErrorCode.values())
                .map(DomainErrorCode::getCode)
                .collect(Collectors.toSet());

        assertEquals(DomainErrorCode.values().length, uniqueCodes.size());
    }

    @Test
    void shouldMapNewQuestionAndSystemErrorsToStandardResponseCodes() {
        BusinessException questionException = new BusinessException(DomainErrorCode.QUESTION_REJECT_REASON_REQUIRED);
        BusinessException backupException = new BusinessException(DomainErrorCode.BACKUP_RECORD_NOT_FOUND);

        assertEquals(ApiResponseCode.BAD_REQUEST.getCode(), questionException.getCode());
        assertEquals(1313, questionException.getErrorCode());

        assertEquals(ApiResponseCode.NOT_FOUND.getCode(), backupException.getCode());
        assertEquals(1217, backupException.getErrorCode());
        assertTrue(backupException.getMessage().contains("备份记录"));
    }

    @Test
    void shouldMapSharedAuthAndFormatErrorsToStandardResponseCodes() {
        BusinessException authenticationException = new BusinessException(DomainErrorCode.AUTHENTICATION_REQUIRED);
        BusinessException timeFormatException = new BusinessException(DomainErrorCode.DATE_TIME_FORMAT_INVALID);
        BusinessException questionTypeException = new BusinessException(DomainErrorCode.QUESTION_TYPE_UNSUPPORTED);
        BusinessException backupQueryException = new BusinessException(DomainErrorCode.SYSTEM_BACKUP_QUERY_FORBIDDEN);
        BusinessException backupTypeException = new BusinessException(DomainErrorCode.SYSTEM_BACKUP_TYPE_INVALID);

        assertEquals(ApiResponseCode.UNAUTHORIZED.getCode(), authenticationException.getCode());
        assertEquals(1001, authenticationException.getErrorCode());

        assertEquals(ApiResponseCode.BAD_REQUEST.getCode(), timeFormatException.getCode());
        assertEquals(1005, timeFormatException.getErrorCode());

        assertEquals(ApiResponseCode.BAD_REQUEST.getCode(), questionTypeException.getCode());
        assertEquals(1006, questionTypeException.getErrorCode());

        assertEquals(ApiResponseCode.FORBIDDEN.getCode(), backupQueryException.getCode());
        assertEquals(1218, backupQueryException.getErrorCode());

        assertEquals(ApiResponseCode.BAD_REQUEST.getCode(), backupTypeException.getCode());
        assertEquals(1219, backupTypeException.getErrorCode());
    }
}
