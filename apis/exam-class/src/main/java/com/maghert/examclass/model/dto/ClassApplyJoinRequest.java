package com.maghert.examclass.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClassApplyJoinRequest {

    @NotBlank(message = "班级码不能为空")
    private String classCode;

    @NotNull(message = "studentId 不能为空")
    private Long studentId;

    private String remark;
}
