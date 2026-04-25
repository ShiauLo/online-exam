package com.maghert.examquestion.constants;

public final class QuestionRoleConstants {

    public static final int SUPER_ADMIN = 1;
    public static final int ADMIN = 2;
    public static final int TEACHER = 3;
    public static final int STUDENT = 4;
    public static final int AUDITOR = 5;
    public static final int OPERATOR = 6;

    private QuestionRoleConstants() {
    }

    public static boolean isAdmin(Integer roleId) {
        return roleId != null && (roleId == SUPER_ADMIN || roleId == ADMIN);
    }

    public static boolean isTeacher(Integer roleId) {
        return roleId != null && roleId == TEACHER;
    }

    public static boolean isAuditor(Integer roleId) {
        return roleId != null && roleId == AUDITOR;
    }
}
