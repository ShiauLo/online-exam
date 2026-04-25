package com.maghert.examsystem.context;

public record RequestContext(Long userId, Integer roleId, String requestId) {
}
