package com.maghert.examclass.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ClassQueryRequest {
    private Long classId;
    private Long teacherId;
    private Long studentId;
    private String keyword;
    private String status;

    @Min(value = 1, message = "pageNum 不能小于1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "pageSize 不能小于1")
    @Max(value = 100, message = "pageSize 不能大于100")
    private Integer pageSize = 10;
}
