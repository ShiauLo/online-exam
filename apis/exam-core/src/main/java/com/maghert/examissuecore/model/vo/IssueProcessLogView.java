package com.maghert.examissuecore.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class IssueProcessLogView {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long logId;
    private String action;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long operatorId;
    private String operatorName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long fromHandlerId;
    private String fromHandlerName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long toHandlerId;
    private String toHandlerName;
    private String content;
    private LocalDateTime occurredAt;
}
