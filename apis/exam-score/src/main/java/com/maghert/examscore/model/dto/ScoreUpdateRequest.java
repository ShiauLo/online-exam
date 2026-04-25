package com.maghert.examscore.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScoreUpdateRequest {

    @NotNull(message = "scoreId 不能为空")
    private Long scoreId;

    @NotNull(message = "newScore 不能为空")
    private Integer newScore;

    @NotBlank(message = "reason 不能为空")
    private String reason;

    @NotNull(message = "approverId 不能为空")
    private Long approverId;
}
