package com.maghert.examcore.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("exam_class")
public class ClassSnapshotEntity {

    @TableId(value = "class_id")
    private Long classId;

    @TableField("class_name")
    private String className;

    @TableField("teacher_id")
    private Long teacherId;

    @TableField("created_by")
    private Long createdBy;

    private String status;
}
