package com.maghert.examquestion.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuestionToggleStatusRequest {

    @NotNull(message = "questionId is required")
    private Long questionId;

    @NotNull(message = "isDisabled is required")
    private Boolean isDisabled;
}
