package com.maghert.examcore.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExamUpdateParamsRequest {

    @NotNull(message = "examId 不能为空")
    private Long examId;

    private Integer duration;

    private String startTime;
}
