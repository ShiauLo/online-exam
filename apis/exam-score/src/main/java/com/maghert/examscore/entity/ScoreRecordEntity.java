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
@TableName("exam_score_record")
public class ScoreRecordEntity {

    @TableId(value = "score_id", type = IdType.INPUT)
    private Long scoreId;

    @TableField("exam_id")
    private Long examId;

    @TableField("exam_name")
    private String examName;

    @TableField("paper_id")
    private Long paperId;

    @TableField("paper_name")
    private String paperName;

    @TableField("class_id")
    private Long classId;

    @TableField("class_name")
    private String className;

    @TableField("student_id")
    private Long studentId;

    @TableField("student_name")
    private String studentName;

    @TableField("total_score")
    private Integer totalScore;

    @TableField("objective_score")
    private Integer objectiveScore;

    @TableField("subjective_score")
    private Integer subjectiveScore;

    private String status;

    @TableField("submitted_at")
    private LocalDateTime submittedAt;

    @TableField("published_at")
    private LocalDateTime publishedAt;

    @TableField("request_id")
    private String requestId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
