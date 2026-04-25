package com.maghert.examissuecore.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IssueCloseRequest {

    @NotNull(message = "issueId 不能为空")
    private Long issueId;

    @NotBlank(message = "confirmResult 不能为空")
    private String confirmResult;

    private String comment;
}
