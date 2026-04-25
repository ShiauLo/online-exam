package com.maghert.examcore.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("exam_class_member")
public class ClassMemberSnapshotEntity {

    @TableId("member_id")
    private Long memberId;

    @TableField("class_id")
    private Long classId;

    @TableField("student_id")
    private Long studentId;

    @TableField("status")
    private String status;
}
