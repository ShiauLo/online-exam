package com.maghert.examresource.context;

public record RequestContext(Long userId, Integer roleId, String requestId) {
}
