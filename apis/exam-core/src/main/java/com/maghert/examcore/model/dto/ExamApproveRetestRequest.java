package com.maghert.examcore.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExamApproveRetestRequest {

    @NotNull(message = "retestApplyId 不能为空")
    private Long retestApplyId;

    @NotBlank(message = "approveResult 不能为空")
    private String approveResult;

    private String reason;
}
