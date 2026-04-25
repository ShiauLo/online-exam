package com.maghert.examcommon.utils;

import com.maghert.examcommon.constants.AuthConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

import java.util.UUID;

public final class RequestIdUtils {

    private RequestIdUtils() {
    }

    public static String resolveOrGenerate(HttpServletRequest request) {
        return resolveOrGenerate(
                request.getHeader(AuthConstants.INTERNAL_REQUEST_ID_HEADER),
                request.getHeader(AuthConstants.REQUEST_ID_HEADER));
    }

    public static String resolveOrGenerate(HttpHeaders headers) {
        return resolveOrGenerate(
                headers.getFirst(AuthConstants.INTERNAL_REQUEST_ID_HEADER),
                headers.getFirst(AuthConstants.REQUEST_ID_HEADER));
    }

    public static String resolveOrGenerate(String internalRequestId, String externalRequestId) {
        if (StringUtils.hasText(internalRequestId)) {
            return internalRequestId;
        }
        if (StringUtils.hasText(externalRequestId)) {
            return externalRequestId;
        }
        return generate();
    }

    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
