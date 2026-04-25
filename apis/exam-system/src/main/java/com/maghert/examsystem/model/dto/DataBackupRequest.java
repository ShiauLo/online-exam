package com.maghert.examsystem.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DataBackupRequest {
    @NotBlank(message = "backupType 不能为空")
    private String backupType;
    private String remark;
}
