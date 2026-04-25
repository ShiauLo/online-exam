package com.maghert.examissuecore.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IssueQueryRequest {

    private Long issueId;

    private String type;

    private String status;

    private Long reporterId;

    private Long handlerId;

    @NotNull(message = "pageNum 不能为空")
    @Min(value = 1, message = "pageNum 必须大于 0")
    private Long pageNum;

    @NotNull(message = "pageSize 不能为空")
    @Min(value = 1, message = "pageSize 必须大于 0")
    private Long pageSize;
}
