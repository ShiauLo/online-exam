package com.maghert.examsystem.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AlarmSettingRequest {
    @NotBlank(message = "alarmType 不能为空")
    private String alarmType;

    @NotBlank(message = "threshold 不能为空")
    private String threshold;

    @NotEmpty(message = "recipients 不能为空")
    private List<String> recipients;
}
