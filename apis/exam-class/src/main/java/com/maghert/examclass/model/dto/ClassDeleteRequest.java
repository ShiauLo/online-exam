package com.maghert.examclass.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClassDeleteRequest {

    @NotNull(message = "classId 不能为空")
    private Long classId;
}
