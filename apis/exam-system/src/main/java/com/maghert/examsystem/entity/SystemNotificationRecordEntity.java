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
@TableName("system_notification_record")
public class SystemNotificationRecordEntity {

    @TableId(value = "notification_id", type = IdType.INPUT)
    private Long notificationId;

    @TableField("event_type")
    private String eventType;

    @TableField("target_type")
    private String targetType;

    @TableField("target_id")
    private String targetId;

    private String status;

    private String channel;

    private String payload;

    @TableField("operator_id")
    private Long operatorId;

    @TableField("request_id")
    private String requestId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
