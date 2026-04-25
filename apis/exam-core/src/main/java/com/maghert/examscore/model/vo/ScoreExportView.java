package com.maghert.examscore.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ScoreExportView {

    private String fileKey;
    private String fileName;
    private Integer recordCount;
    private Boolean includeAnalysis;
    private LocalDateTime generatedAt;
}
