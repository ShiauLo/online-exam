package com.maghert.examscore.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScoreQuestionDetailView {

    private Long questionId;
    private Integer sortNo;
    private String questionType;
    private String questionStem;
    private String studentAnswer;
    private String correctAnswer;
    private Integer assignedScore;
    private Integer score;
    private Boolean correct;
    private String reviewComment;
}
