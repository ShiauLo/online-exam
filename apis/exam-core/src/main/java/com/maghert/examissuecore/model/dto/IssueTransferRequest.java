package com.maghert.examissuecore.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IssueTransferRequest {

    @NotNull(message = "issueId 不能为空")
    private Long issueId;

    private Long fromHandlerId;

    @NotNull(message = "toHandlerId 不能为空")
    private Long toHandlerId;

    @NotBlank(message = "reason 不能为空")
    private String reason;
}
