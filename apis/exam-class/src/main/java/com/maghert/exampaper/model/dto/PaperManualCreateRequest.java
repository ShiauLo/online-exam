package com.maghert.exampaper.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PaperManualCreateRequest {

    @NotBlank(message = "paperName 不能为空")
    private String paperName;

    @NotEmpty(message = "questionIds 不能为空")
    private List<Long> questionIds;

    @NotNull(message = "examTime 不能为空")
    @Min(value = 1, message = "examTime 必须大于 0")
    private Integer examTime;

    @NotNull(message = "passScore 不能为空")
    @Min(value = 1, message = "passScore 必须大于 0")
    private Integer passScore;
}
