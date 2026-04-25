package com.maghert.examsystem.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
@TableName(value = "system_role", autoResultMap = true)
public class SystemRoleEntity {

    @TableId(value = "role_id", type = IdType.INPUT)
    private Long roleId;

    @TableField("role_name")
    private String roleName;

    @TableField(value = "permission_ids", typeHandler = JacksonTypeHandler.class)
    private List<String> permissionIds = new ArrayList<>();

    @TableField("template_id")
    private Long templateId;

    @TableField("created_by")
    private Long createdBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
