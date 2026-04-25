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
@TableName("exam_instance_student")
public class ExamStudentRelationEntity {

    @TableId(value = "relation_id", type = IdType.INPUT)
    private Long relationId;

    @TableField("exam_id")
    private Long examId;

    @TableField("class_id")
    private Long classId;

    @TableField("student_id")
    private Long studentId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
