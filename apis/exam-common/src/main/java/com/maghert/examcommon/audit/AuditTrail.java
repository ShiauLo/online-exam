package com.maghert.examcommon.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditTrail {

    private String actionType;
    private Long operatorId;
    private String targetType;
    private String targetId;
    private String requestId;
    private String detail;
    private LocalDateTime occurredAt;
}
