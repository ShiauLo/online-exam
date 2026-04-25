package com.maghert.examquestion.context;

import com.maghert.examcommon.constants.AuthConstants;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.exception.DomainErrorCode;
import com.maghert.examcommon.exception.TokenException;
import com.maghert.examcommon.utils.JwtUtils;
import com.maghert.examcommon.utils.RequestIdUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class HeaderQuestionAccessContext implements QuestionAccessContext {

    private final JwtUtils jwtUtils;

    public HeaderQuestionAccessContext(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public Long requireUserId() throws BusinessException {
        HttpServletRequest request = currentRequest();
        String headerValue = firstNonBlank(
                request.getHeader(AuthConstants.USER_ID_HEADER),
                request.getHeader(AuthConstants.USER_ID_HEADER.toLowerCase()));
        if (headerValue != null) {
            return parseLong(headerValue);
        }
        String authorization = request.getHeader(AuthConstants.AUTHORIZATION_HEADER);
        if (authorization == null || authorization.isBlank()) {
            throw new BusinessException(DomainErrorCode.AUTHENTICATION_REQUIRED);
        }
        try {
            return jwtUtils.getUserIdFromToken(normalizeToken(authorization));
        } catch (TokenException e) {
            throw new BusinessException(DomainErrorCode.TOKEN_INVALID);
        }
    }

    @Override
    public Integer requireRoleId() throws BusinessException {
        HttpServletRequest request = currentRequest();
        String headerValue = firstNonBlank(
                request.getHeader(AuthConstants.ROLE_ID_HEADER),
                request.getHeader(AuthConstants.ROLE_ID_HEADER.toLowerCase()));
        if (headerValue != null) {
            return parseInteger(headerValue);
        }
        String authorization = request.getHeader(AuthConstants.AUTHORIZATION_HEADER);
        if (authorization == null || authorization.isBlank()) {
            throw new BusinessException(DomainErrorCode.AUTHENTICATION_REQUIRED);
        }
        try {
            Integer roleId = jwtUtils.getRoleIdFromToken(normalizeToken(authorization));
            if (roleId == null) {
                throw new BusinessException(DomainErrorCode.ROLE_CONTEXT_REQUIRED);
            }
            return roleId;
        } catch (TokenException e) {
            throw new BusinessException(DomainErrorCode.TOKEN_INVALID);
        }
    }

    @Override
    public String currentRequestId() {
        return RequestIdUtils.resolveOrGenerate(currentRequest());
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            throw new IllegalStateException("request context not available");
        }
        return servletAttributes.getRequest();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Long parseLong(String value) throws BusinessException {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new BusinessException(DomainErrorCode.REQUEST_CONTEXT_INVALID);
        }
    }

    private Integer parseInteger(String value) throws BusinessException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new BusinessException(DomainErrorCode.REQUEST_CONTEXT_INVALID);
        }
    }

    private String normalizeToken(String authorization) {
        if (authorization.toLowerCase().startsWith(AuthConstants.BEARER_PREFIX.toLowerCase())) {
            return authorization.substring(AuthConstants.BEARER_PREFIX.length());
        }
        return authorization;
    }
}
