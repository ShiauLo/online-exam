package com.maghert.exampaper.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PaperUpdateRequest {

    @NotNull(message = "paperId 不能为空")
    private Long paperId;

    private String paperName;

    private List<Long> questionIds;

    @Min(value = 1, message = "examTime 必须大于 0")
    private Integer examTime;

    @Min(value = 1, message = "passScore 必须大于 0")
    private Integer passScore;
}
