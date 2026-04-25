package com.maghert.exampaper.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PaperView {

    private Long paperId;
    private String paperName;
    private String status;
    private String sourceType;
    private Long creatorId;
    private Integer examTime;
    private Integer passScore;
    private Integer totalScore;
    private List<Long> questionIds;
    private List<Long> classIds;
    private LocalDateTime scheduledExamTime;
    private LocalDateTime publishedAt;
    private LocalDateTime recycledAt;
}
