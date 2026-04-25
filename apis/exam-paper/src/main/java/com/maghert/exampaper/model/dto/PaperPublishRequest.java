package com.maghert.exampaper.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PaperPublishRequest {

    @NotNull(message = "paperId 不能为空")
    private Long paperId;

    @NotBlank(message = "examTime 不能为空")
    private String examTime;

    @NotEmpty(message = "classIds 不能为空")
    private List<Long> classIds;
}
