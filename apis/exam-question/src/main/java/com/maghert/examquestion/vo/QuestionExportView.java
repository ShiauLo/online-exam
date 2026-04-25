package com.maghert.examquestion.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class QuestionExportView {

    private String fileKey;
    private String fileName;
    private Integer recordCount;
    private Boolean masked;
    private LocalDateTime generatedAt;
}
