package com.maghert.examquestion.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuestionDeleteRequest {

    @NotNull(message = "questionId is required")
    private Long questionId;
}
