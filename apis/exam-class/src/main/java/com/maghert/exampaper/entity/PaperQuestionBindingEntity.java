package com.maghert.exampaper.entity;

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
@TableName("exam_paper_question")
public class PaperQuestionBindingEntity {

    @TableId(value = "binding_id", type = IdType.INPUT)
    private Long bindingId;

    @TableField("paper_id")
    private Long paperId;

    @TableField("question_id")
    private Long questionId;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField("assigned_score")
    private Integer assignedScore;

    @TableField("question_type")
    private String questionType;

    private Integer difficulty;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
