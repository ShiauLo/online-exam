package com.maghert.examcommon.auth;

import java.util.Map;

public final class RoleMappings {

    private static final Map<Integer, String> INTERNAL_CODES = Map.of(
            1, "SUPER_ADMIN",
            2, "ADMIN",
            3, "TEACHER",
            4, "STUDENT",
            5, "AUDITOR",
            6, "OPERATOR");

    private static final Map<Integer, String> EXTERNAL_CODES = Map.of(
            1, "super_admin",
            2, "admin",
            3, "teacher",
            4, "student",
            5, "auditor",
            6, "ops");

    private RoleMappings() {
    }

    public static String toInternalCode(Integer roleId) {
        return INTERNAL_CODES.getOrDefault(roleId, "UNKNOWN");
    }

    public static String toExternalCode(Integer roleId) {
        return EXTERNAL_CODES.getOrDefault(roleId, "unknown");
    }

    public static boolean isTeacher(Integer roleId) {
        return Integer.valueOf(3).equals(roleId);
    }

    public static boolean isStudent(Integer roleId) {
        return Integer.valueOf(4).equals(roleId);
    }

    public static boolean isAdmin(Integer roleId) {
        return Integer.valueOf(1).equals(roleId) || Integer.valueOf(2).equals(roleId);
    }

    public static boolean isAuditor(Integer roleId) {
        return Integer.valueOf(5).equals(roleId);
    }

    public static boolean isOps(Integer roleId) {
        return Integer.valueOf(6).equals(roleId);
    }
}
