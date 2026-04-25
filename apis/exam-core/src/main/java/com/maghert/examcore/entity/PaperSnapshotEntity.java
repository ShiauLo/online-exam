package com.maghert.examcore.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("exam_paper")
public class PaperSnapshotEntity {

    @TableId(value = "paper_id")
    private Long paperId;

    @TableField("paper_name")
    private String paperName;

    @TableField("creator_id")
    private Long creatorId;

    private String status;
}
