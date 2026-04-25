package com.maghert.examissuecore.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IssueTrackRequest {

    @NotNull(message = "issueId 不能为空")
    private Long issueId;
}
