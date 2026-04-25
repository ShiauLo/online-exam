package com.maghert.examscore.context;

public record RequestContext(Long userId, Integer roleId, String requestId) {
}
