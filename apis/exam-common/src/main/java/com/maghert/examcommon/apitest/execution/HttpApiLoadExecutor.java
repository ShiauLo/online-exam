package com.maghert.examcommon.apitest.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maghert.examcommon.apitest.model.ApiEndpointSpec;
import com.maghert.examcommon.apitest.model.ApiScenarioSpec;
import com.maghert.examcommon.apitest.model.ApiTestSuite;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class HttpApiLoadExecutor {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpApiLoadExecutor(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.objectMapper = objectMapper;
    }

    public LoadSummary execute(ApiTestSuite suite, URI baseUri, LoadProfile profile) throws Exception {
        List<Callable<InvocationResult>> tasks = new ArrayList<>();
        for (ApiEndpointSpec endpoint : suite.getEndpoints()) {
            for (ApiScenarioSpec scenario : endpoint.getScenarios()) {
                if (!scenario.isIntegrationExecutable()) {
                    continue;
                }
                for (int i = 0; i < profile.iterationsPerScenario(); i++) {
                    tasks.add(() -> invoke(baseUri, endpoint, scenario));
                }
            }
        }

        ExecutorService executor = Executors.newFixedThreadPool(profile.concurrency());
        try {
            List<Future<InvocationResult>> futures = executor.invokeAll(tasks);
            int successCount = 0;
            long totalLatencyMillis = 0L;
            for (Future<InvocationResult> future : futures) {
                InvocationResult result = future.get();
                if (result.success()) {
                    successCount++;
                }
                totalLatencyMillis += result.latencyMillis();
            }
            return new LoadSummary(tasks.size(), successCount, totalLatencyMillis);
        } finally {
            executor.shutdownNow();
        }
    }

    private InvocationResult invoke(URI baseUri, ApiEndpointSpec endpoint, ApiScenarioSpec scenario) throws IOException, InterruptedException {
        long start = System.currentTimeMillis();
        HttpRequest.Builder builder = HttpRequest.newBuilder(baseUri.resolve(endpoint.getPath()))
                .timeout(Duration.ofSeconds(10));
        String body = objectMapper.writeValueAsString(scenario.getRequestBody());
        switch (endpoint.getHttpMethod()) {
            case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(body));
            case "PUT" -> builder.PUT(HttpRequest.BodyPublishers.ofString(body));
            case "DELETE" -> builder.method("DELETE", HttpRequest.BodyPublishers.ofString(body));
            default -> builder.GET();
        }
        builder.header("Content-Type", "application/json");
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        long latency = System.currentTimeMillis() - start;
        boolean success = scenario.getExpectedStatus() == null || scenario.getExpectedStatus().equals(response.statusCode());
        return new InvocationResult(success, latency);
    }

    public record LoadProfile(int concurrency, int iterationsPerScenario) {
    }

    public record LoadSummary(int totalInvocations, int successCount, long totalLatencyMillis) {
    }

    private record InvocationResult(boolean success, long latencyMillis) {
    }
}

