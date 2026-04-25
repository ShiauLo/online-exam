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
@TableName("exam_score_appeal")
public class ScoreAppealEntity {

    @TableId(value = "appeal_id", type = IdType.INPUT)
    private Long appealId;

    @TableField("score_id")
    private Long scoreId;

    @TableField("exam_id")
    private Long examId;

    @TableField("class_id")
    private Long classId;

    @TableField("student_id")
    private Long studentId;

    @TableField("question_id")
    private Long questionId;

    @TableField("appeal_reason")
    private String appealReason;

    private String status;

    @TableField("request_id")
    private String requestId;

    @TableField("handler_id")
    private Long handlerId;

    @TableField("handle_result")
    private String handleResult;

    @TableField("handle_reason")
    private String handleReason;

    @TableField("handled_at")
    private LocalDateTime handledAt;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
