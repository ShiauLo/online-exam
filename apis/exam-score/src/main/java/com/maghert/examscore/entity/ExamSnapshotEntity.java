package com.maghert.examscore.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("exam_instance")
public class ExamSnapshotEntity {

    @TableField("exam_id")
    private Long examId;

    @TableField("exam_name")
    private String examName;

    @TableField("paper_id")
    private Long paperId;

    @TableField("creator_id")
    private Long creatorId;
}
