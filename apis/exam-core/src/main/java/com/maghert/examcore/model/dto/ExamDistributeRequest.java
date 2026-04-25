package com.maghert.examcore.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ExamDistributeRequest {

    @NotNull(message = "examId 不能为空")
    private Long examId;

    private List<Long> studentIds;
}
