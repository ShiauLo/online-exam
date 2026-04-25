package com.maghert.examclass.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClassQuitRequest {

    @NotNull(message = "classId 不能为空")
    private Long classId;

    @NotNull(message = "studentId 不能为空")
    private Long studentId;
}
