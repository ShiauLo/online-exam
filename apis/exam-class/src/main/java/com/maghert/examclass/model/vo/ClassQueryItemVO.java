package com.maghert.examclass.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassQueryItemVO {
    private Long classId;
    private String classCode;
    private String className;
    private String description;
    private Long teacherId;
    private Boolean forced;
    private String status;
    private Long approvedMemberCount;
    private Long pendingMemberCount;
}
