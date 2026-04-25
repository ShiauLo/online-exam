package com.maghert.examsystem.notification;

import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maghert.examsystem.entity.SystemNotificationRecordEntity;
import com.maghert.examsystem.repository.SystemDomainRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OutboxSystemNotificationPublisher implements SystemNotificationPublisher {

    private static final String DEFAULT_STATUS = "PENDING";
    private static final String FAILED_STATUS = "FAILED";
    private static final String DEFAULT_CHANNEL = "OUTBOX";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SystemDomainRepository repository;
    private final Snowflake snowflake;
    private final ObjectMapper objectMapper;

    public OutboxSystemNotificationPublisher(SystemDomainRepository repository, Snowflake snowflake, ObjectMapper objectMapper) {
        this.repository = repository;
        this.snowflake = snowflake;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(SystemNotificationEvent event) {
        LocalDateTime occurredAt = event.getOccurredAt() == null ? LocalDateTime.now() : event.getOccurredAt();
        String payload = serializePayload(event, occurredAt);
        String status = payload.startsWith("{\"summary\":\"通知序列化失败") ? FAILED_STATUS : DEFAULT_STATUS;

        repository.saveNotification(new SystemNotificationRecordEntity()
                .setNotificationId(snowflake.nextId())
                .setEventType(event.getEventType())
                .setTargetType(event.getTargetType())
                .setTargetId(event.getTargetId())
                .setStatus(status)
                .setChannel(DEFAULT_CHANNEL)
                .setPayload(payload)
                .setOperatorId(event.getOperatorId())
                .setRequestId(event.getRequestId())
                .setCreateTime(occurredAt)
                .setUpdateTime(occurredAt));
    }

    private String serializePayload(SystemNotificationEvent event, LocalDateTime occurredAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", event.getEventType());
        payload.put("targetId", event.getTargetId());
        payload.put("operatorId", event.getOperatorId());
        payload.put("requestId", event.getRequestId());
        payload.put("occurredAt", occurredAt.format(DATE_TIME_FORMATTER));
        payload.put("summary", StringUtils.hasText(event.getSummary()) ? event.getSummary() : "-");
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            return "{\"summary\":\"通知序列化失败\",\"eventType\":\"%s\"}".formatted(event.getEventType());
        }
    }
}
