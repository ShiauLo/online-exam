package com.maghert.examsystem.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BackupTaskAcceptedVO {
    private Long backupId;
    private String status;
    private String lifecycleStage;
}
