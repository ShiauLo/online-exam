package com.maghert.examquestion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuestionCategoryQueryRequest {

    private Long categoryId;
    private Long parentId;
    private Long creatorId;
    private Boolean isPersonal;
    private Boolean isDisabled;
    private String keyword;

    @NotNull(message = "pageNum is required")
    @Min(value = 1, message = "pageNum must be >= 1")
    private Integer pageNum;

    @NotNull(message = "pageSize is required")
    @Min(value = 1, message = "pageSize must be >= 1")
    @Max(value = 100, message = "pageSize must be <= 100")
    private Integer pageSize;
}
