package com.maghert.examscore.entity;

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
@TableName("exam_score_change_log")
public class ScoreChangeLogEntity {

    @TableId(value = "change_log_id", type = IdType.INPUT)
    private Long changeLogId;

    @TableField("score_id")
    private Long scoreId;

    @TableField("exam_id")
    private Long examId;

    @TableField("class_id")
    private Long classId;

    @TableField("student_id")
    private Long studentId;

    @TableField("operator_id")
    private Long operatorId;

    @TableField("approver_id")
    private Long approverId;

    @TableField("previous_total_score")
    private Integer previousTotalScore;

    @TableField("new_total_score")
    private Integer newTotalScore;

    private String reason;

    @TableField("request_id")
    private String requestId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
