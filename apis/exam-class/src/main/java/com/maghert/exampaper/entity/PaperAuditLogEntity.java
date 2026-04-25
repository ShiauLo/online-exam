package com.maghert.exampaper.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("exam_paper_audit_log")
public class PaperAuditLogEntity {

    @TableId(value = "audit_log_id", type = IdType.INPUT)
    private Long auditLogId;

    @TableField("paper_id")
    private Long paperId;

    @TableField("auditor_id")
    private Long auditorId;

    @TableField("audit_result")
    private String auditResult;

    private String reason;

    @TableField("request_id")
    private String requestId;

    @TableField("audit_time")
    private LocalDateTime auditTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
