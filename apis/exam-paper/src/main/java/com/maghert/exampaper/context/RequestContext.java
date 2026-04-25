package com.maghert.exampaper.context;

public record RequestContext(Long userId, Integer roleId, String requestId) {
}
