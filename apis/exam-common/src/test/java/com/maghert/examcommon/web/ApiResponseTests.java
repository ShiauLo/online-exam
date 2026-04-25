package com.maghert.examcommon.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiResponseTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializeSuccessFailureAndValidationResponses() throws Exception {
        String successJson = objectMapper.writeValueAsString(ApiResponse.ok("ok").withRequestId("req-1"));
        String validationJson = objectMapper.writeValueAsString(
                ApiResponse.fail(ApiResponseCode.BAD_REQUEST, "参数错误")
                        .withRequestId("req-2")
                        .withErrors(List.of("field required")));
        String conflictJson = objectMapper.writeValueAsString(
                ApiResponse.fail(ApiResponseCode.CONFLICT, "状态冲突")
                        .withRequestId("req-3"));

        assertTrue(successJson.contains("\"code\":200"));
        assertTrue(successJson.contains("\"msg\":\"success\""));
        assertTrue(successJson.contains("\"requestId\":\"req-1\""));

        assertTrue(validationJson.contains("\"code\":400"));
        assertTrue(validationJson.contains("\"msg\":\"参数错误\""));
        assertTrue(validationJson.contains("\"errors\":[\"field required\"]"));

        assertTrue(conflictJson.contains("\"code\":409"));
        assertTrue(conflictJson.contains("\"msg\":\"状态冲突\""));
        assertTrue(conflictJson.contains("\"timestamp\":"));
    }
}
