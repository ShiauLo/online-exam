package com.maghert.exampaper.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaperDeleteRequest {

    @NotNull(message = "paperId 不能为空")
    private Long paperId;
}
