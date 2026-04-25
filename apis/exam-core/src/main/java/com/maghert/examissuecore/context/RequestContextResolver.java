package com.maghert.examissuecore.context;

import com.maghert.examcommon.constants.AuthConstants;
import com.maghert.examcommon.utils.RequestIdUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RequestContextResolver {

    public RequestContext resolve(HttpServletRequest request) {
        return new RequestContext(
                toLong(request.getHeader(AuthConstants.USER_ID_HEADER)),
                toInteger(request.getHeader(AuthConstants.ROLE_ID_HEADER)),
                RequestIdUtils.resolveOrGenerate(request));
    }

    private Long toLong(String value) {
        try {
            return StringUtils.hasText(value) ? Long.valueOf(value) : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Integer toInteger(String value) {
        try {
            return StringUtils.hasText(value) ? Integer.valueOf(value) : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
