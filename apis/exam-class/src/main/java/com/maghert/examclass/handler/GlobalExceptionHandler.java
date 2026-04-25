package com.maghert.examclass.handler;

import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
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

@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.profiles.active:prod}")
    private String activeEnv;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<String> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        ApiResponse<Void> response = ApiResponse.<Void>fail(400, "参数错误")
                .withRequestId(resolveRequestId(request))
                .withErrors(errors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        List<String> errors = exception.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .toList();
        ApiResponse<Void> response = ApiResponse.<Void>fail(400, "参数错误")
                .withRequestId(resolveRequestId(request))
                .withErrors(errors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception, HttpServletRequest request) {
        int code = exception.getCode() != null ? exception.getCode() : 400;
        ApiResponse<Void> response = ApiResponse.<Void>fail(code, exception.getMessage())
                .withRequestId(resolveRequestId(request));
        return ResponseEntity.status(Objects.requireNonNull(toStatus(code))).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(
            NoResourceFoundException exception,
            HttpServletRequest request) {
        ApiResponse<Void> response = ApiResponse.<Void>fail(404, "请求路径不存在")
                .withRequestId(resolveRequestId(request));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception, HttpServletRequest request) {
        String message = "dev".equalsIgnoreCase(activeEnv) ? exception.getMessage() : "服务器内部错误";
        ApiResponse<Void> response = ApiResponse.<Void>fail(500, message)
                .withRequestId(resolveRequestId(request));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private HttpStatus toStatus(int code) {
        return switch (code) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 409 -> HttpStatus.CONFLICT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = request.getHeader("Request-Id");
        }
        return requestId == null || requestId.isBlank()
                ? UUID.randomUUID().toString().replace("-", "")
                : requestId;
    }
}
