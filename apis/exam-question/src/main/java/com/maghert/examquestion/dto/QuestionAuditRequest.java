package com.maghert.examquestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuestionAuditRequest {

    @NotNull(message = "questionId is required")
    private Long questionId;

    @NotBlank(message = "auditResult is required")
    private String auditResult;

    private String reason;
}
