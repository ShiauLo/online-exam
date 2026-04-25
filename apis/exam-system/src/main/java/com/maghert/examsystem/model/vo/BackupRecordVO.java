package com.maghert.examsystem.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BackupRecordVO {
    private Long backupId;
    private String backupType;
    private String status;
    private String remark;
    private Long operatorId;
    private String createTime;
    private String updateTime;
    private Boolean canRestore;
    private String lifecycleStage;
}
