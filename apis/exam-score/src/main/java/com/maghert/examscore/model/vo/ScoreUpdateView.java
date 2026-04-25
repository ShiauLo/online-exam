package com.maghert.examscore.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ScoreUpdateView {

    private Long scoreId;
    private Integer previousTotalScore;
    private Integer totalScore;
    private Integer objectiveScore;
    private Integer subjectiveScore;
    private Long approverId;
    private LocalDateTime updatedAt;
}
