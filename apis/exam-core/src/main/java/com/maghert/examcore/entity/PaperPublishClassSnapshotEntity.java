package com.maghert.examcore.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("exam_paper_publish_class")
public class PaperPublishClassSnapshotEntity {

    @TableId(value = "relation_id")
    private Long relationId;

    @TableField("paper_id")
    private Long paperId;

    @TableField("class_id")
    private Long classId;
}
