package com.maghert.examcommon.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditTrailTests {

    @Test
    void shouldSerializeFrozenAuditFields() throws Exception {
        AuditTrail trail = AuditTrail.builder()
                .actionType("question.audit")
                .operatorId(2001L)
                .targetType("question")
                .targetId("3001")
                .requestId("req-1")
                .detail("approved")
                .build();

        String json = new ObjectMapper().writeValueAsString(trail);

        assertTrue(json.contains("\"actionType\":\"question.audit\""));
        assertTrue(json.contains("\"operatorId\":2001"));
        assertTrue(json.contains("\"requestId\":\"req-1\""));
    }
}
