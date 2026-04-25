package com.maghert.examclass.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClassUpdateRequest {

    @NotNull(message = "classId 不能为空")
    private Long classId;

    @Size(max = 30, message = "班级名称长度不能超过30")
    private String className;

    @Size(max = 500, message = "班级描述长度不能超过500")
    private String description;

    private Long teacherId;
    private String status;
    private Boolean forced;
}
