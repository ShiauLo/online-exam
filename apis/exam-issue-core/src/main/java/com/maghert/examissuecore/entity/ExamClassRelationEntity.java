package com.maghert.examissuecore.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("exam_instance_class")
public class ExamClassRelationEntity {

    @TableId("relation_id")
    private Long relationId;

    @TableField("exam_id")
    private Long examId;

    @TableField("class_id")
    private Long classId;
}
