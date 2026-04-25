package com.maghert.examaccount.handler;

import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.utils.RequestIdUtils;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examcommon.web.ApiResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.profiles.active:prod}")
    private String activeEnv;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e,
                                                                       HttpServletRequest request) {
        String requestId = RequestIdUtils.resolveOrGenerate(request);
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        log.warn("[validation-exception] requestId={}, path={}, errors={}", requestId, request.getRequestURI(), errors);
        ApiResponse<Void> body = ApiResponse.<Void>fail(ApiResponseCode.BAD_REQUEST)
                .withRequestId(requestId)
                .withErrors(errors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e,
                                                                       HttpServletRequest request) {
        String requestId = RequestIdUtils.resolveOrGenerate(request);
        List<String> errors = e.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .toList();
        log.warn("[constraint-violation] requestId={}, path={}, errors={}", requestId, request.getRequestURI(), errors);
        ApiResponse<Void> body = ApiResponse.<Void>fail(ApiResponseCode.BAD_REQUEST)
                .withRequestId(requestId)
                .withErrors(errors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        String requestId = RequestIdUtils.resolveOrGenerate(request);
        log.warn("[business-exception] requestId={}, path={}, code={}, message={}",
                requestId, request.getRequestURI(), e.getCode(), e.getMessage(), e);
        int code = e.getCode() != null ? e.getCode() : ApiResponseCode.BAD_REQUEST.getCode();
        ApiResponse<Void> body = ApiResponse.<Void>fail(code, e.getMessage()).withRequestId(requestId);
        return ResponseEntity.status(Objects.requireNonNull(resolveStatus(code))).body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException e,
                                                                   HttpServletRequest request) {
        String requestId = RequestIdUtils.resolveOrGenerate(request);
        log.warn("[no-resource-found] requestId={}, path={}", requestId, request.getRequestURI());
        ApiResponse<Void> body = ApiResponse.<Void>fail(404, "请求路径不存在").withRequestId(requestId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        String requestId = RequestIdUtils.resolveOrGenerate(request);
        log.error("[runtime-exception] requestId={}, path={}, message={}", requestId, request.getRequestURI(), e.getMessage(), e);
        String msg = isDev() && e.getMessage() != null ? e.getMessage() : ApiResponseCode.INTERNAL_SERVER_ERROR.getDefaultMessage();
        ApiResponse<Void> body = ApiResponse.<Void>fail(ApiResponseCode.INTERNAL_SERVER_ERROR, msg).withRequestId(requestId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllException(Exception e, HttpServletRequest request) {
        String requestId = RequestIdUtils.resolveOrGenerate(request);
        log.error("[unknown-exception] requestId={}, path={}, message={}", requestId, request.getRequestURI(), e.getMessage(), e);
        String msg = isDev() && e.getMessage() != null ? e.getMessage() : ApiResponseCode.INTERNAL_SERVER_ERROR.getDefaultMessage();
        ApiResponse<Void> body = ApiResponse.<Void>fail(ApiResponseCode.INTERNAL_SERVER_ERROR, msg).withRequestId(requestId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private HttpStatus resolveStatus(int code) {
        return switch (code) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 409 -> HttpStatus.CONFLICT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private boolean isDev() {
        return "dev".equalsIgnoreCase(activeEnv);
    }
}
