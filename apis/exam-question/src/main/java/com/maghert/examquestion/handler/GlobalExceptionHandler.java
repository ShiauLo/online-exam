package com.maghert.examquestion.handler;

import com.maghert.examcommon.constants.AuthConstants;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.web.ApiResponse;
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
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.profiles.active:prod}")
    private String activeEnv;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e,
                                                                       HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        ApiResponse<Void> body = ApiResponse.<Void>fail(400, "parameter invalid")
                .withRequestId(requestId)
                .withErrors(errors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e,
                                                                       HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        List<String> errors = e.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .toList();
        ApiResponse<Void> body = ApiResponse.<Void>fail(400, "parameter invalid")
                .withRequestId(requestId)
                .withErrors(errors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        int code = e.getCode() != null ? e.getCode() : 400;
        ApiResponse<Void> body = ApiResponse.<Void>fail(code, e.getMessage()).withRequestId(requestId);
        return ResponseEntity.status(Objects.requireNonNull(resolveStatus(code))).body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException e,
                                                                   HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        ApiResponse<Void> body = ApiResponse.<Void>fail(404, "请求路径不存在").withRequestId(requestId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("[runtime-exception] path={}, message={}", request.getRequestURI(), e.getMessage(), e);
        String requestId = resolveRequestId(request);
        String message = isDev() ? e.getMessage() : "internal server error";
        ApiResponse<Void> body = ApiResponse.<Void>fail(500, message).withRequestId(requestId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknownException(Exception e, HttpServletRequest request) {
        log.error("[unknown-exception] path={}, message={}", request.getRequestURI(), e.getMessage(), e);
        String requestId = resolveRequestId(request);
        String message = isDev() ? e.getMessage() : "internal server error";
        ApiResponse<Void> body = ApiResponse.<Void>fail(500, message).withRequestId(requestId);
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

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(AuthConstants.INTERNAL_REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = request.getHeader(AuthConstants.REQUEST_ID_HEADER);
        }
        return requestId == null || requestId.isBlank()
                ? UUID.randomUUID().toString().replace("-", "")
                : requestId;
    }
}
