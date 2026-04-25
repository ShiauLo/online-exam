package com.maghert.examgateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForbiddenResponseGlobalFilterTests {

    @Test
    void shouldRewriteForbiddenBodyAsStructuredJson() {
        ForbiddenResponseGlobalFilter filter = new ForbiddenResponseGlobalFilter(new ObjectMapper());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/system/config/query").build());

        filter.filter(exchange, serverWebExchange -> {
            serverWebExchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            serverWebExchange.getResponse().getHeaders().setContentType(MediaType.TEXT_PLAIN);
            byte[] bodyBytes = Objects.requireNonNull("forbidden".getBytes(StandardCharsets.UTF_8));
            return serverWebExchange.getResponse().writeWith(Objects.requireNonNull(Mono.just(
                    new DefaultDataBufferFactory().wrap(bodyBytes))));
        }).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        String body = exchange.getResponse().getBodyAsString().block();
        assertTrue(body.contains("\"code\":403"));
        assertTrue(body.contains("\"msg\":\"无权限访问\""));
        assertTrue(body.contains("\"requestId\":\""));
        assertTrue(body.contains("\"timestamp\":"));
    }

    @Test
    void shouldWriteStructuredForbiddenWhenDownstreamOnlySetsStatus() {
        ForbiddenResponseGlobalFilter filter = new ForbiddenResponseGlobalFilter(new ObjectMapper());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/question/export").build());

        filter.filter(exchange, serverWebExchange -> {
            serverWebExchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return serverWebExchange.getResponse().setComplete();
        }).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        String body = exchange.getResponse().getBodyAsString().block();
        assertTrue(body.contains("\"code\":403"));
        assertTrue(body.contains("\"msg\":\"无权限访问\""));
    }
}
