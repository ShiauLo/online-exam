package com.maghert.examcommon.constants;

@Deprecated(forRemoval = false)
public final class ExamHeaderNames {

    @Deprecated(forRemoval = false)
    public static final String USER_ID = AuthConstants.USER_ID_HEADER;
    @Deprecated(forRemoval = false)
    public static final String ROLE_ID = AuthConstants.ROLE_ID_HEADER;
    @Deprecated(forRemoval = false)
    public static final String REQUEST_ID = AuthConstants.INTERNAL_REQUEST_ID_HEADER;
    @Deprecated(forRemoval = false)
    public static final String EXTERNAL_REQUEST_ID = AuthConstants.REQUEST_ID_HEADER;

    private ExamHeaderNames() {
    }
}
