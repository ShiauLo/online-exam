package com.maghert.examquestion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class QuestionUpdateRequest {

    @NotNull(message = "questionId is required")
    private Long questionId;

    private String content;
    private List<String> options;
    private String answer;
    private String analysis;

    @Min(value = 1, message = "difficulty must be >= 1")
    @Max(value = 5, message = "difficulty must be <= 5")
    private Integer difficulty;
}
