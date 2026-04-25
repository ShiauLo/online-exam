package com.maghert.examcore.entity;

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
@TableName("exam_instance_status_log")
public class ExamStatusLogEntity {

    @TableId(value = "log_id", type = IdType.INPUT)
    private Long logId;

    @TableField("exam_id")
    private Long examId;

    @TableField("operator_id")
    private Long operatorId;

    @TableField("operator_role")
    private String operatorRole;

    @TableField("action_type")
    private String actionType;

    @TableField("from_status")
    private String fromStatus;

    @TableField("to_status")
    private String toStatus;

    private String reason;

    @TableField("request_id")
    private String requestId;

    @TableField("operate_time")
    private LocalDateTime operateTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
