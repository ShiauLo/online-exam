package com.maghert.examscore.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScoreHandleAppealRequest {

    @NotNull(message = "appealId 不能为空")
    private Long appealId;

    @NotBlank(message = "handleResult 不能为空")
    private String handleResult;

    @NotBlank(message = "reason 不能为空")
    private String reason;
}
