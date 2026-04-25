package com.maghert.examissuecore.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IssueHandleRequest {

    @NotNull(message = "issueId 不能为空")
    private Long issueId;

    private Long handlerId;

    @NotBlank(message = "result 不能为空")
    private String result;

    private String solution;
}
