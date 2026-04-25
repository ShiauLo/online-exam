package com.maghert.exampaper.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("question_item")
public class QuestionSnapshotEntity {

    @TableId(value = "question_id")
    private Long questionId;

    @TableField("category_id")
    private Long categoryId;

    @TableField("creator_id")
    private Long creatorId;

    @TableField("creator_role_id")
    private Integer creatorRoleId;

    private String content;

    @TableField("question_type")
    private String questionType;

    private Integer difficulty;

    @TableField("audit_status")
    private String auditStatus;

    @TableField("is_disabled")
    private Boolean disabled;

    @TableField("reference_count")
    private Integer referenceCount;
}
