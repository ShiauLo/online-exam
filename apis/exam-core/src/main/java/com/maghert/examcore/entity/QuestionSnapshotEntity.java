package com.maghert.examcore.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("question_item")
public class QuestionSnapshotEntity {

    @TableId("question_id")
    private Long questionId;

    private String content;

    @TableField("question_type")
    private String questionType;

    private String answer;
}
