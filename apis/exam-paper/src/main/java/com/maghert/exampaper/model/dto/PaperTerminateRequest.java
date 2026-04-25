package com.maghert.exampaper.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaperTerminateRequest {

    @NotNull(message = "paperId 不能为空")
    private Long paperId;

    @NotBlank(message = "reason 不能为空")
    private String reason;
}
