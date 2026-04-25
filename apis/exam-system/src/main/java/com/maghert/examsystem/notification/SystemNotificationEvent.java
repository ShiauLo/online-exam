package com.maghert.examsystem.notification;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SystemNotificationEvent {
    private String eventType;
    private String targetType;
    private String targetId;
    private Long operatorId;
    private String requestId;
    private LocalDateTime occurredAt;
    private String summary;
}
