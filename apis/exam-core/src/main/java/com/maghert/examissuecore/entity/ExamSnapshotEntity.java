package com.maghert.examissuecore.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("exam_instance")
public class ExamSnapshotEntity {

    @TableId("exam_id")
    private Long examId;

    @TableField("exam_name")
    private String examName;

    @TableField("creator_id")
    private Long creatorId;

    private String status;
}
