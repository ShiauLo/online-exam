package com.maghert.examclass.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClassRemoveStudentRequest {

    @NotNull(message = "classId 不能为空")
    private Long classId;

    @NotNull(message = "studentId 不能为空")
    private Long studentId;

    @NotBlank(message = "reason 不能为空")
    private String reason;
}
