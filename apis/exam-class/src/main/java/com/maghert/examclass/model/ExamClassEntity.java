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
@TableName("exam_class")
public class ExamClassEntity {
    @TableId(value = "class_id", type = IdType.INPUT)
    private Long classId;
    @TableField("class_code")
    private String classCode;
    @TableField("class_name")
    private String className;
    private String description;
    @TableField("teacher_id")
    private Long teacherId;
    private Boolean forced;
    private String status;
    @TableField("created_by")
    private Long createdBy;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
