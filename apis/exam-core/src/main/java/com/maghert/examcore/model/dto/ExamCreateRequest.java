package com.maghert.examcore.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ExamCreateRequest {

    @NotBlank(message = "examName 不能为空")
    private String examName;

    @NotNull(message = "paperId 不能为空")
    private Long paperId;

    @NotEmpty(message = "classIds 不能为空")
    private List<Long> classIds;

    @NotBlank(message = "startTime 不能为空")
    private String startTime;

    @NotNull(message = "duration 不能为空")
    @Min(value = 1, message = "duration 必须大于 0")
    private Integer duration;
}
