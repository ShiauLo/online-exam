package com.maghert.examsystem.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SystemLogVO {
    private Long logId;
    private String logType;
    private Long operatorId;
    private String operator;
    private String detail;
    private String approverId;
    private String createTime;
}
