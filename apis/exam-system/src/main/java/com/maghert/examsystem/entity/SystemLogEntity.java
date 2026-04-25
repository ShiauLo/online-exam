package com.maghert.examsystem.entity;

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
@TableName("system_log")
public class SystemLogEntity {

    @TableId(value = "log_id", type = IdType.INPUT)
    private Long logId;

    @TableField("log_type")
    private String logType;

    @TableField("operator_id")
    private Long operatorId;

    private String operator;

    private String detail;

    @TableField("approver_id")
    private String approverId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
