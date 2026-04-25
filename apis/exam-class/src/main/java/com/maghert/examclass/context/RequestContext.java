package com.maghert.examclass.context;

public record RequestContext(Long userId, Integer roleId, String requestId) {
}
