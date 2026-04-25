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
@TableName("system_permission_assignment")
public class SystemPermissionAssignmentEntity {

    @TableId(value = "assignment_id", type = IdType.INPUT)
    private Long assignmentId;

    @TableField("account_id")
    private Long accountId;

    @TableField("role_id")
    private Integer roleId;

    @TableField("expire_time")
    private String expireTime;

    @TableField("assigned_by")
    private Long assignedBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
