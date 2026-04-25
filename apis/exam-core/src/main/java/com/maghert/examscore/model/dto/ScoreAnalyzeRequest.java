package com.maghert.examscore.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ScoreAnalyzeRequest {

    @NotNull(message = "examId 不能为空")
    private Long examId;

    private Long classId;

    private List<String> dimensions;
}
