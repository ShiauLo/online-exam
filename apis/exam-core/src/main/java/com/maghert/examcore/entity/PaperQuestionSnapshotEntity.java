package com.maghert.examcore.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("exam_paper_question")
public class PaperQuestionSnapshotEntity {

    @TableId("binding_id")
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
}
