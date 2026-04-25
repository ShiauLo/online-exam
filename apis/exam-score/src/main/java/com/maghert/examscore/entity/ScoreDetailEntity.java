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
@TableName("exam_score_detail")
public class ScoreDetailEntity {

    @TableId(value = "detail_id", type = IdType.INPUT)
    private Long detailId;

    @TableField("score_id")
    private Long scoreId;

    @TableField("exam_id")
    private Long examId;

    @TableField("student_id")
    private Long studentId;

    @TableField("question_id")
    private Long questionId;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField("question_type")
    private String questionType;

    @TableField("question_stem")
    private String questionStem;

    @TableField("student_answer")
    private String studentAnswer;

    @TableField("correct_answer")
    private String correctAnswer;

    @TableField("assigned_score")
    private Integer assignedScore;

    @TableField("score")
    private Integer score;

    @TableField("is_correct")
    private Boolean correct;

    @TableField("review_comment")
    private String reviewComment;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
