package com.maghert.exampaper.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaperAuditRequest {

    @NotNull(message = "paperId 不能为空")
    private Long paperId;

    @NotBlank(message = "auditResult 不能为空")
    private String auditResult;

    private String reason;
}
