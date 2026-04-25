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
@TableName("exam_instance_retest_apply")
public class ExamRetestApplyEntity {

    @TableId(value = "retest_apply_id", type = IdType.INPUT)
    private Long retestApplyId;

    @TableField("exam_id")
    private Long examId;

    @TableField("class_id")
    private Long classId;

    @TableField("student_id")
    private Long studentId;

    private String status;

    @TableField("apply_reason")
    private String applyReason;

    @TableField("decision_reason")
    private String decisionReason;

    @TableField("reviewed_by")
    private Long reviewedBy;

    @TableField("request_id")
    private String requestId;

    @TableField("review_time")
    private LocalDateTime reviewTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
