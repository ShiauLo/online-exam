package com.maghert.examgateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maghert.examcommon.constants.AuthConstants;
import com.maghert.examcommon.exception.TokenException;
import com.maghert.examcommon.utils.JwtUtils;
import com.maghert.examcommon.utils.RequestIdUtils;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examcommon.web.ApiResponseCode;
import com.maghert.examgateway.config.AuthProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String MISSING_TOKEN_MESSAGE = "未登录或缺少 token";

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private final AuthProperties authProperties;
    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    public AuthGlobalFilter(AuthProperties authProperties, JwtUtils jwtUtils, ObjectMapper objectMapper) {
        this.authProperties = authProperties;
        this.jwtUtils = jwtUtils;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            return chain.filter(exchange);
        }
        if (isExclude(request.getPath().value())) {
            return chain.filter(exchange);
        }

        String authorization = request.getHeaders().getFirst(AuthConstants.AUTHORIZATION_HEADER);
        String token = extractToken(authorization);
        if (!StringUtils.hasText(token)) {
            return writeUnauthorized(exchange.getResponse(), request, MISSING_TOKEN_MESSAGE);
        }

        try {
            Long userId = jwtUtils.getUserIdFromToken(token);
            Integer roleId = jwtUtils.getRoleIdFromToken(token);
            String requestId = RequestIdUtils.resolveOrGenerate(request.getHeaders());

            ServerHttpRequest.Builder builder = request.mutate()
                    .header(AuthConstants.USER_ID_HEADER, String.valueOf(userId))
                    .header(AuthConstants.INTERNAL_REQUEST_ID_HEADER, requestId);
            if (roleId != null) {
                builder.header(AuthConstants.ROLE_ID_HEADER, String.valueOf(roleId));
            }
            if (StringUtils.hasText(authorization)) {
                builder.header(AuthConstants.AUTHORIZATION_HEADER, authorization);
            }
            return chain.filter(exchange.mutate().request(builder.build()).build());
        } catch (TokenException e) {
            return writeUnauthorized(exchange.getResponse(), request, e.getMessage());
        }
    }

    private boolean isExclude(String path) {
        if (CollectionUtils.isEmpty(authProperties.getIgnorePaths())) {
            return false;
        }
        String checkedPath = Objects.requireNonNull(path);
        for (String pathPattern : authProperties.getIgnorePaths()) {
            String checkedPathPattern = Objects.requireNonNull(pathPattern);
            if (antPathMatcher.match(checkedPathPattern, checkedPath)) {
                return true;
            }
        }
        return false;
    }

    private String extractToken(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        if (authorization.startsWith(AuthConstants.BEARER_PREFIX)) {
            String token = authorization.substring(AuthConstants.BEARER_PREFIX.length()).trim();
            return StringUtils.hasText(token) ? token : null;
        }
        return authorization.trim();
    }

    private Mono<Void> writeUnauthorized(ServerHttpResponse response, ServerHttpRequest request, String message) {
        return writeError(response, request, HttpStatus.UNAUTHORIZED, ApiResponseCode.UNAUTHORIZED, message);
    }

    @SuppressWarnings("unused")
    private Mono<Void> writeForbidden(ServerHttpResponse response, ServerHttpRequest request, String message) {
        return writeError(response, request, HttpStatus.FORBIDDEN, ApiResponseCode.FORBIDDEN, message);
    }

    private Mono<Void> writeError(ServerHttpResponse response, ServerHttpRequest request, HttpStatus status,
                                  ApiResponseCode responseCode, String message) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(Objects.requireNonNull(MediaType.APPLICATION_JSON));

        ApiResponse<Void> payload = ApiResponse.<Void>fail(responseCode, message)
                .withRequestId(RequestIdUtils.resolveOrGenerate(request.getHeaders()));

        DataBuffer buffer = response.bufferFactory().wrap(Objects.requireNonNull(toJsonBytes(payload, responseCode)));
        return response.writeWith(Objects.requireNonNull(Mono.just(buffer)));
    }

    private byte[] toJsonBytes(ApiResponse<Void> payload, ApiResponseCode responseCode) {
        try {
            return objectMapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            String fallback = "{\"code\":" + responseCode.getCode()
                    + ",\"msg\":\"" + responseCode.getDefaultMessage() + "\"}";
            return fallback.getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
