package com.maghert.examclass.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClassCreateRequest {

    @NotBlank(message = "班级名称不能为空")
    @Size(max = 30, message = "班级名称长度不能超过30")
    private String className;

    @Size(max = 500, message = "班级描述长度不能超过500")
    private String description;

    private Long teacherId;
    private Boolean forced;
}
