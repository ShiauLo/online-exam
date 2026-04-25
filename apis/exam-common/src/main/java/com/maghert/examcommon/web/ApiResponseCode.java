package com.maghert.examcommon.web;

import lombok.Getter;

@Getter
public enum ApiResponseCode {

    SUCCESS(200, "success"),
    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录态失效"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "请求状态冲突"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误");

    private final int code;
    private final String defaultMessage;

    ApiResponseCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}
