package com.maghert.examquestion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class QuestionCreateRequest {

    @NotBlank(message = "content is required")
    private String content;

    @NotBlank(message = "type is required")
    private String type;

    private List<String> options;

    @NotBlank(message = "answer is required")
    private String answer;

    @NotNull(message = "categoryId is required")
    private Long categoryId;

    private String analysis;

    @NotNull(message = "difficulty is required")
    @Min(value = 1, message = "difficulty must be >= 1")
    @Max(value = 5, message = "difficulty must be <= 5")
    private Integer difficulty;
}
