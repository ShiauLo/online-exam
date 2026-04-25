package com.maghert.examscore.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("exam_paper")
public class PaperSnapshotEntity {

    @TableField("paper_id")
    private Long paperId;

    @TableField("pass_score")
    private Integer passScore;

    @TableField("total_score")
    private Integer totalScore;
}
