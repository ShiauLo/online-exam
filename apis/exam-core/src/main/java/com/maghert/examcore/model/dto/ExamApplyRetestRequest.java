package com.maghert.examcore.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExamApplyRetestRequest {

    @NotNull(message = "examId 不能为空")
    private Long examId;

    @NotNull(message = "studentId 不能为空")
    private Long studentId;

    @NotBlank(message = "reason 不能为空")
    private String reason;
}
