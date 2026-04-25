package com.maghert.examquestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuestionCategoryUpsertRequest {

    private Long categoryId;

    @NotBlank(message = "name is required")
    private String name;

    private Long parentId;

    @NotNull(message = "isPersonal is required")
    private Boolean isPersonal;

    private Boolean isDisabled;
}
