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
@TableName("exam_class_member")
public class ExamClassMemberEntity {
    @TableId(value = "member_id", type = IdType.INPUT)
    private Long memberId;
    @TableField("class_id")
    private Long classId;
    @TableField("student_id")
    private Long studentId;
    @TableField("status")
    private ClassMemberStatus status;
    private String remark;
    private String reason;
    @TableField("operated_by")
    private Long operatedBy;
    @TableField(value = "apply_time", fill = FieldFill.INSERT)
    private LocalDateTime applyTime;
    @TableField("decision_time")
    private LocalDateTime decisionTime;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
