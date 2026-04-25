package com.maghert.examsystem.context;

import com.maghert.examcommon.constants.AuthConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Component
public class RequestContextResolver {

    public RequestContext resolve(HttpServletRequest request) {
        Long userId = toLong(request.getHeader(AuthConstants.USER_ID_HEADER));
        Integer roleId = toInteger(request.getHeader(AuthConstants.ROLE_ID_HEADER));
        String requestId = firstNonBlank(
                request.getHeader(AuthConstants.INTERNAL_REQUEST_ID_HEADER),
                request.getHeader(AuthConstants.REQUEST_ID_HEADER),
                UUID.randomUUID().toString().replace("-", ""));
        return new RequestContext(userId, roleId, requestId);
    }

    private Long toLong(String value) {
        try {
            return StringUtils.hasText(value) ? Long.valueOf(value) : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer toInteger(String value) {
        try {
            return StringUtils.hasText(value) ? Integer.valueOf(value) : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String firstNonBlank(String first, String second, String fallback) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        if (StringUtils.hasText(second)) {
            return second;
        }
        return fallback;
    }
}
