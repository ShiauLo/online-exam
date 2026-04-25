package com.maghert.examscore.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ScorePublishView {

    private Long examId;
    private Long classId;
    private Integer publishedCount;
    private String status;
    private LocalDateTime publishedAt;
}
