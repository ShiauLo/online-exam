package com.maghert.examcommon.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private Integer code;
    private String msg;
    private T data;
    private String requestId;
    private Long timestamp;
    private List<String> errors;

    public ApiResponse() {
        this.timestamp = Instant.now().toEpochMilli();
    }

    public static <T> ApiResponse<T> ok() {
        return build(ApiResponseCode.SUCCESS);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return build(ApiResponseCode.SUCCESS, data);
    }

    public static <T> ApiResponse<T> build(ApiResponseCode responseCode) {
        return build(responseCode, null);
    }

    public static <T> ApiResponse<T> build(ApiResponseCode responseCode, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(responseCode.getCode());
        response.setMsg(responseCode.getDefaultMessage());
        response.setData(data);
        return response;
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMsg(message);
        return response;
    }

    public static <T> ApiResponse<T> fail(ApiResponseCode responseCode) {
        return build(responseCode);
    }

    public static <T> ApiResponse<T> fail(ApiResponseCode responseCode, String message) {
        ApiResponse<T> response = build(responseCode);
        response.setMsg(message);
        return response;
    }

    public ApiResponse<T> withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public ApiResponse<T> withErrors(List<String> errors) {
        this.errors = errors;
        return this;
    }

    public String getMessage() {
        return msg;
    }

    public void setMessage(String message) {
        this.msg = message;
    }
}
