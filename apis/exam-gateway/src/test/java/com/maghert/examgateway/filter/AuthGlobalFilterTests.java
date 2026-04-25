package com.maghert.examgateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maghert.examcommon.constants.AuthConstants;
import com.maghert.examcommon.exception.InvalidTokenException;
import com.maghert.examcommon.utils.JwtUtils;
import com.maghert.examgateway.config.AuthProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class AuthGlobalFilterTests {

    private static final String MISSING_TOKEN = "\u672a\u767b\u5f55\u6216\u7f3a\u5c11 token";

    @Test
    void shouldBypassNestedIgnorePath() {
        JwtUtils jwtUtils = Mockito.mock(JwtUtils.class);
        AuthProperties properties = new AuthProperties();
        properties.setIgnorePaths(List.of("/api/account/send/**"));
        AuthGlobalFilter filter = new AuthGlobalFilter(properties, jwtUtils, new ObjectMapper());

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/account/send/verifycode").build());
        AtomicReference<String> pathRef = new AtomicReference<>();
        GatewayFilterChain chain = serverWebExchange -> {
            pathRef.set(serverWebExchange.getRequest().getPath().value());
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertEquals("/api/account/send/verifycode", pathRef.get());
        Mockito.verifyNoInteractions(jwtUtils);
    }

    @Test
    void shouldBypassOptionsPreflightWithoutToken() {
        JwtUtils jwtUtils = Mockito.mock(JwtUtils.class);
        AuthProperties properties = new AuthProperties();
        properties.setIgnorePaths(List.of("/api/account/send/**"));
        AuthGlobalFilter filter = new AuthGlobalFilter(properties, jwtUtils, new ObjectMapper());

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.OPTIONS, "/api/account/login")
                        .header("Origin", "http://127.0.0.1:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .build());
        AtomicReference<String> methodRef = new AtomicReference<>();
        GatewayFilterChain chain = serverWebExchange -> {
            methodRef.set(String.valueOf(serverWebExchange.getRequest().getMethod()));
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertEquals("OPTIONS", methodRef.get());
        Mockito.verifyNoInteractions(jwtUtils);
    }

    @Test
    void shouldReturnStructured401WhenTokenMissing() {
        JwtUtils jwtUtils = Mockito.mock(JwtUtils.class);
        AuthProperties properties = new AuthProperties();
        properties.setIgnorePaths(List.of("/api/account/send/**"));
        AuthGlobalFilter filter = new AuthGlobalFilter(properties, jwtUtils, new ObjectMapper());

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/account/query").build());

        filter.filter(exchange, serverWebExchange -> Mono.empty()).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        String body = exchange.getResponse().getBodyAsString().block();
        assertTrue(body.contains("\"code\":401"));
        assertTrue(body.contains("\"msg\":\"" + MISSING_TOKEN + "\""));
        assertTrue(body.contains("\"requestId\":\""));
        assertTrue(body.contains("\"timestamp\":"));
    }

    @Test
    void shouldReturnStructured401WhenTokenInvalid() throws Exception {
        JwtUtils jwtUtils = Mockito.mock(JwtUtils.class);
        when(jwtUtils.getUserIdFromToken(anyString())).thenThrow(new InvalidTokenException("token invalid"));
        AuthProperties properties = new AuthProperties();
        properties.setIgnorePaths(List.of("/api/account/send/**"));
        AuthGlobalFilter filter = new AuthGlobalFilter(properties, jwtUtils, new ObjectMapper());

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/account/query")
                        .header(AuthConstants.AUTHORIZATION_HEADER, "bad-token")
                        .build());

        filter.filter(exchange, serverWebExchange -> Mono.empty()).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        String body = exchange.getResponse().getBodyAsString().block();
        assertTrue(body.contains("\"code\":401"));
        assertTrue(body.contains("\"msg\":\"token invalid\""));
    }

    @Test
    void shouldPassUserIdHeaderWhenTokenValid() throws Exception {
        JwtUtils jwtUtils = Mockito.mock(JwtUtils.class);
        when(jwtUtils.getUserIdFromToken("valid-token")).thenReturn(1001L);
        when(jwtUtils.getRoleIdFromToken("valid-token")).thenReturn(3);
        AuthProperties properties = new AuthProperties();
        properties.setIgnorePaths(List.of("/api/account/send/**"));
        AuthGlobalFilter filter = new AuthGlobalFilter(properties, jwtUtils, new ObjectMapper());

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/account/query")
                        .header(AuthConstants.REQUEST_ID_HEADER, "req-100")
                        .header(AuthConstants.AUTHORIZATION_HEADER, "valid-token")
                        .build());
        AtomicReference<String> userIdHeader = new AtomicReference<>();
        AtomicReference<String> roleIdHeader = new AtomicReference<>();
        AtomicReference<String> requestIdHeader = new AtomicReference<>();
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        GatewayFilterChain chain = serverWebExchange -> {
            userIdHeader.set(serverWebExchange.getRequest().getHeaders().getFirst(AuthConstants.USER_ID_HEADER));
            roleIdHeader.set(serverWebExchange.getRequest().getHeaders().getFirst(AuthConstants.ROLE_ID_HEADER));
            requestIdHeader.set(serverWebExchange.getRequest().getHeaders().getFirst(AuthConstants.INTERNAL_REQUEST_ID_HEADER));
            authorizationHeader.set(serverWebExchange.getRequest().getHeaders().getFirst(AuthConstants.AUTHORIZATION_HEADER));
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertEquals("1001", userIdHeader.get());
        assertEquals("3", roleIdHeader.get());
        assertEquals("req-100", requestIdHeader.get());
        assertEquals("valid-token", authorizationHeader.get());
    }

    @Test
    void shouldAcceptBearerTokenAndGenerateRequestIdWhenMissing() throws Exception {
        JwtUtils jwtUtils = Mockito.mock(JwtUtils.class);
        when(jwtUtils.getUserIdFromToken("bearer-token")).thenReturn(2002L);
        when(jwtUtils.getRoleIdFromToken("bearer-token")).thenReturn(5);
        AuthProperties properties = new AuthProperties();
        properties.setIgnorePaths(List.of("/api/account/send/**"));
        AuthGlobalFilter filter = new AuthGlobalFilter(properties, jwtUtils, new ObjectMapper());

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/question/query")
                        .header(AuthConstants.AUTHORIZATION_HEADER, "Bearer bearer-token")
                        .build());
        AtomicReference<String> requestIdHeader = new AtomicReference<>();
        AtomicReference<String> userIdHeader = new AtomicReference<>();
        GatewayFilterChain chain = serverWebExchange -> {
            requestIdHeader.set(serverWebExchange.getRequest().getHeaders().getFirst(AuthConstants.INTERNAL_REQUEST_ID_HEADER));
            userIdHeader.set(serverWebExchange.getRequest().getHeaders().getFirst(AuthConstants.USER_ID_HEADER));
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertEquals("2002", userIdHeader.get());
        assertTrue(requestIdHeader.get() != null && !requestIdHeader.get().isBlank());
    }
}
