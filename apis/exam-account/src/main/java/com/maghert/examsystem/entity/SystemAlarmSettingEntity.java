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
@TableName(value = "system_alarm_setting", autoResultMap = true)
public class SystemAlarmSettingEntity {

    @TableId(value = "alarm_id", type = IdType.INPUT)
    private Long alarmId;

    @TableField("alarm_type")
    private String alarmType;

    private String threshold;

    @TableField(value = "recipients", typeHandler = JacksonTypeHandler.class)
    private List<String> recipients = new ArrayList<>();

    @TableField("updated_by")
    private Long updatedBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
