package com.maghert.examscore.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScoreAutoScoreRequest {

    @NotNull(message = "examId 不能为空")
    private Long examId;

    @NotNull(message = "studentId 不能为空")
    private Long studentId;
}
