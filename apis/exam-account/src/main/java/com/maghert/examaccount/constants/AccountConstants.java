package com.maghert.examaccount.constants;

public class AccountConstants {

    public static final String REDIS_VERIFY_CODE_LOGIN = "verifyCode:login:";

    public static final String REPLACE_VERIFY_CODE="verifyCode";

    public static final int VERIFY_CODE_LENGTH = 6 ;

    public static final String  OK = "OK";

    public static final String REDIS_ACCESS_TOKEN = "access_token:";

    public static final String REDIS_REFRESH_TOKEN = "refresh_token:";

    public static final Integer STUDENT_ROLE_ID = 4;

    public static final String USER_ID = "userId";

    public static final String ROLE_ID = "roleId";

    public static final String REPLACE_VERIFY_TTL = "TTL";

    public static final Long VERIFY_CODE_TTL = 5L;

    public static final String CODE_PASSWORD_LOGIN = "password_login";
    public static final String CODE_ONE_KEY_LOGIN = "one_key_login";

}
