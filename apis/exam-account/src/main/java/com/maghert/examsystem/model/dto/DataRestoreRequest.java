package com.maghert.examsystem.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DataRestoreRequest {
    @NotNull(message = "backupId 不能为空")
    private Long backupId;

    @NotBlank(message = "verifyCode1 不能为空")
    private String verifyCode1;

    @NotBlank(message = "verifyCode2 不能为空")
    private String verifyCode2;
}
