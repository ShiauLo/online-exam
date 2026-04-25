package com.maghert.examscore.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScoreApplyRecheckRequest {

    @NotNull(message = "examId 不能为空")
    private Long examId;

    @NotNull(message = "studentId 不能为空")
    private Long studentId;

    @NotNull(message = "questionId 不能为空")
    private Long questionId;

    @NotBlank(message = "reason 不能为空")
    private String reason;
}
