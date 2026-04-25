package com.maghert.examissuecore.entity;

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
@TableName("exam_issue_process_log")
public class IssueProcessLogEntity {

    @TableId(value = "log_id", type = IdType.INPUT)
    private Long logId;

    @TableField("issue_id")
    private Long issueId;

    private String action;

    @TableField("operator_id")
    private Long operatorId;

    @TableField("from_handler_id")
    private Long fromHandlerId;

    @TableField("to_handler_id")
    private Long toHandlerId;

    private String content;

    @TableField("audit_trail")
    private String auditTrail;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
