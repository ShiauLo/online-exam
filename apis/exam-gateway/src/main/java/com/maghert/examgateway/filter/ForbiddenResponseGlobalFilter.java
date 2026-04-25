package com.maghert.examgateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maghert.examcommon.utils.RequestIdUtils;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examcommon.web.ApiResponseCode;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
public class ForbiddenResponseGlobalFilter implements GlobalFilter, Ordered {

    private static final String FORBIDDEN_MESSAGE = "无权限访问";

    private final ObjectMapper objectMapper;

    public ForbiddenResponseGlobalFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse originalResponse = exchange.getResponse();
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public @NonNull Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
                if (HttpStatus.FORBIDDEN.equals(getStatusCode())) {
                    return Objects.requireNonNull(replaceBody(body, request));
                }
                return Objects.requireNonNull(super.writeWith(body));
            }

            @Override
            public @NonNull Mono<Void> writeAndFlushWith(@NonNull Publisher<? extends Publisher<? extends DataBuffer>> body) {
                if (HttpStatus.FORBIDDEN.equals(getStatusCode())) {
                    Flux<DataBuffer> flattenedBody = Flux.from(body).flatMapSequential(publisher -> publisher);
                    return Objects.requireNonNull(replaceBody(
                            Objects.requireNonNull(flattenedBody),
                            request));
                }
                return Objects.requireNonNull(super.writeAndFlushWith(body));
            }

            @Override
            public @NonNull Mono<Void> setComplete() {
                if (HttpStatus.FORBIDDEN.equals(getStatusCode())) {
                    return Objects.requireNonNull(writeForbidden(getDelegate(), request));
                }
                return Objects.requireNonNull(super.setComplete());
            }

            private @NonNull Mono<Void> replaceBody(@NonNull Publisher<? extends DataBuffer> body,
                                                    ServerHttpRequest request) {
                return Objects.requireNonNull(Flux.from(body)
                        .doOnNext(DataBufferUtils::release)
                        .then(writeForbidden(getDelegate(), request)));
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    private Mono<Void> writeForbidden(ServerHttpResponse response, ServerHttpRequest request) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(Objects.requireNonNull(MediaType.APPLICATION_JSON));

        ApiResponse<Void> payload = ApiResponse.<Void>fail(ApiResponseCode.FORBIDDEN, FORBIDDEN_MESSAGE)
                .withRequestId(RequestIdUtils.resolveOrGenerate(request.getHeaders()));
        DataBuffer buffer = response.bufferFactory().wrap(Objects.requireNonNull(toJsonBytes(payload)));
        return response.writeWith(Objects.requireNonNull(Mono.just(buffer)));
    }

    private byte[] toJsonBytes(ApiResponse<Void> payload) {
        try {
            return objectMapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            String fallback = "{\"code\":403,\"msg\":\"" + FORBIDDEN_MESSAGE + "\"}";
            return fallback.getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
