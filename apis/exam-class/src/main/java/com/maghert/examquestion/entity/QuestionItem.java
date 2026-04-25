package com.maghert.examquestion.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
@TableName(value = "question_item", autoResultMap = true)
public class QuestionItem {

    @TableId(value = "question_id", type = IdType.INPUT)
    private Long id;
    @TableField("category_id")
    private Long categoryId;
    @TableField("creator_id")
    private Long creatorId;
    @TableField("creator_role_id")
    private Integer creatorRoleId;
    private String content;
    @TableField("question_type")
    private String questionType;
    @TableField(value = "options", typeHandler = JacksonTypeHandler.class)
    private List<String> options = new ArrayList<>();
    private String answer;
    private String analysis;
    private Integer difficulty;
    @TableField("audit_status")
    private String auditStatus;
    @TableField("is_disabled")
    private Boolean disabled;
    @TableField("reference_count")
    private Integer referenceCount;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
