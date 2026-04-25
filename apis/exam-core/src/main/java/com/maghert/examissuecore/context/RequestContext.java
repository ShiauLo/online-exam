package com.maghert.examissuecore.context;

public record RequestContext(Long userId, Integer roleId, String requestId) {
}
