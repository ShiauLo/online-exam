package com.maghert.examscore.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScorePublishRequest {

    @NotNull(message = "examId 不能为空")
    private Long examId;

    @NotNull(message = "classId 不能为空")
    private Long classId;
}
