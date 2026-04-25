package com.maghert.examscore.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScoreAutoScoreView {

    private Long examId;
    private Long studentId;
    private Integer objectiveScore;
    private Integer totalScore;
    private String status;
    private Integer scoredQuestionCount;
}
