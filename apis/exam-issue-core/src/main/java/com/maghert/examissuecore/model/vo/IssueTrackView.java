package com.maghert.examissuecore.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class IssueTrackView {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long issueId;
    private String type;
    private String title;
    private String status;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long reporterId;
    private String reporterName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long currentHandlerId;
    private String currentHandlerName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long examId;
    private String examName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long classId;
    private String className;
    private String latestResult;
    private String latestSolution;
    private List<String> imgUrls;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<IssueProcessLogView> logs;
}
