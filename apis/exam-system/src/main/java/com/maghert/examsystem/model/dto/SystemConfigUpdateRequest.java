package com.maghert.examsystem.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SystemConfigUpdateRequest {
    @NotBlank(message = "configKey 不能为空")
    private String configKey;

    @NotBlank(message = "configValue 不能为空")
    private String configValue;

    @NotBlank(message = "verifyCode 不能为空")
    private String verifyCode;
}
