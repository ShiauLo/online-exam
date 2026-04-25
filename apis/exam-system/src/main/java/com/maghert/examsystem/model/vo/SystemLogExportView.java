package com.maghert.examsystem.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SystemLogExportView {

    private String fileKey;
    private String fileName;
    private Integer recordCount;
    private LocalDateTime generatedAt;
}
