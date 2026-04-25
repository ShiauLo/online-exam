package com.maghert.exampaper.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("question_category")
public class QuestionCategorySnapshotEntity {

    @TableId(value = "category_id")
    private Long categoryId;

    @TableField("owner_id")
    private Long ownerId;

    @TableField("is_personal")
    private Boolean personal;

    private Integer status;
}
