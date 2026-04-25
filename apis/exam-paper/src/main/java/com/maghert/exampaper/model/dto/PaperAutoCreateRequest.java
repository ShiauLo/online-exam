package com.maghert.exampaper.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class PaperAutoCreateRequest {

    @NotBlank(message = "paperName 不能为空")
    private String paperName;

    @NotEmpty(message = "typeRatio 不能为空")
    private Map<String, Integer> typeRatio;

    @NotEmpty(message = "difficultyRatio 不能为空")
    private Map<String, Integer> difficultyRatio;

    private Map<String, Integer> knowledgeRatio;

    @NotNull(message = "totalScore 不能为空")
    @Min(value = 1, message = "totalScore 必须大于 0")
    private Integer totalScore;

    @Min(value = 1, message = "examTime 必须大于 0")
    private Integer examTime;

    @Min(value = 1, message = "passScore 必须大于 0")
    private Integer passScore;
}
