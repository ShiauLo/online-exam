package com.maghert.examcore.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExamRetestApplyView {

    private Long retestApplyId;

    private Long examId;

    private Long studentId;

    private String status;

    private LocalDateTime createdAt;
}
