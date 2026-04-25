package com.maghert.examquestion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuestionQueryRequest {

    private Long questionId;
    private Long categoryId;
    private Long creatorId;
    private String keyword;
    private String type;
    private Integer difficulty;
    private String auditStatus;
    private Boolean isDisabled;
    private Boolean isReferenced;

    @NotNull(message = "pageNum is required")
    @Min(value = 1, message = "pageNum must be >= 1")
    private Integer pageNum;

    @NotNull(message = "pageSize is required")
    @Min(value = 1, message = "pageSize must be >= 1")
    @Max(value = 100, message = "pageSize must be <= 100")
    private Integer pageSize;
}
