package com.maghert.examscore.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScoreManualScoreView {

    private Long examId;
    private Long studentId;
    private Long questionId;
    private Integer score;
    private Integer assignedScore;
    private String reviewComment;
    private Integer subjectiveScore;
    private Integer totalScore;
    private String status;
}
