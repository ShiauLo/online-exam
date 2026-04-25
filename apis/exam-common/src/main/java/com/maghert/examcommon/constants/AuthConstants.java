package com.maghert.examcommon.constants;

public final class AuthConstants {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String REQUEST_ID_HEADER = "Request-Id";
    public static final String INTERNAL_REQUEST_ID_HEADER = "X-Request-Id";
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String ROLE_ID_HEADER = "X-Role-Id";
    public static final String BEARER_PREFIX = "Bearer ";

    public static final String USER_ID_CLAIM = "userId";
    public static final String ROLE_ID_CLAIM = "roleId";

    private AuthConstants() {
    }
}
