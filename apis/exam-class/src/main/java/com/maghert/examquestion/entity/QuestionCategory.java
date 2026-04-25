package com.maghert.examquestion.entity;

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
@TableName("question_category")
public class QuestionCategory {

    @TableId(value = "category_id", type = IdType.INPUT)
    private Long id;
    private String name;
    @TableField("parent_id")
    private Long parentId;
    @TableField("is_personal")
    private Boolean personal;
    @TableField("owner_id")
    private Long ownerId;
    private Integer status;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
