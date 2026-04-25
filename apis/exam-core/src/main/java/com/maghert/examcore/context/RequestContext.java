package com.maghert.examcore.context;

public record RequestContext(Long userId, Integer roleId, String requestId) {
}
