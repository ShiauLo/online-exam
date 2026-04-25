package com.maghert.examclass.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@TableName("exam_class_audit_log")
public class ExamClassAuditLogEntity {
    @TableId(value = "audit_log_id", type = IdType.INPUT)
    private Long auditLogId;
    @TableField("class_id")
    private Long classId;
    @TableField("member_id")
    private Long memberId;
    @TableField("operator_id")
    private Long operatorId;
    @TableField("action_type")
    private String actionType;
    @TableField("target_student_id")
    private Long targetStudentId;
    private String detail;
    @TableField("request_id")
    private String requestId;
    @TableField("operate_time")
    private LocalDateTime operateTime;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
