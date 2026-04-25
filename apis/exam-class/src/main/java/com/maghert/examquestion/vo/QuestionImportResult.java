package com.maghert.examquestion.vo;

import lombok.Data;

import java.util.List;

@Data
public class QuestionImportResult {

    private int totalCount;
    private int successCount;
    private int failedCount;
    private int importedCount;
    private List<String> rowErrors;
    private List<String> errors;

    public QuestionImportResult(int successCount, int failedCount, List<String> rowErrors) {
        this.totalCount = successCount + failedCount;
        this.successCount = successCount;
        this.failedCount = failedCount;
        this.importedCount = successCount;
        this.rowErrors = rowErrors;
        this.errors = rowErrors;
    }
}
