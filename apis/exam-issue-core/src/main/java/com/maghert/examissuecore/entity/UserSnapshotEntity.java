package com.maghert.examissuecore.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("sys_user")
public class UserSnapshotEntity {

    @TableId("id")
    private Long userId;

    @TableField("real_name")
    private String realName;

    @TableField("role_id")
    private Integer roleId;
}
