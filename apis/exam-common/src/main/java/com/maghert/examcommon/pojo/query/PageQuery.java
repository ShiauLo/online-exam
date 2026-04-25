package com.maghert.examcommon.pojo.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PageQuery {

    @Min(value = 1, message = "pageNum must be greater than or equal to 1")
    private Long pageNum = 1L;

    @Min(value = 1, message = "pageSize must be greater than or equal to 1")
    @Max(value = 100, message = "pageSize must be less than or equal to 100")
    private Long pageSize = 10L;
}
