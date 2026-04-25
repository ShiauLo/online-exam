package com.maghert.examclass.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ClassApproveJoinRequest {

    @jakarta.validation.constraints.NotNull(message = "classId 不能为空")
    private Long classId;

    private Long studentId;

    private List<Long> studentIds;

    @NotBlank(message = "approveResult 不能为空")
    private String approveResult;

    private String reason;
}
