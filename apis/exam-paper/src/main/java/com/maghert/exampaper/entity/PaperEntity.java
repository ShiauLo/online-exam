package com.maghert.exampaper.entity;

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
@TableName("exam_paper")
public class PaperEntity {

    @TableId(value = "paper_id", type = IdType.INPUT)
    private Long paperId;

    @TableField("paper_name")
    private String paperName;

    @TableField("creator_id")
    private Long creatorId;

    @TableField("creator_role_id")
    private Integer creatorRoleId;

    private String status;

    @TableField("source_type")
    private String sourceType;

    @TableField("exam_time")
    private Integer examTime;

    @TableField("pass_score")
    private Integer passScore;

    @TableField("total_score")
    private Integer totalScore;

    @TableField("scheduled_exam_time")
    private LocalDateTime scheduledExamTime;

    @TableField("published_at")
    private LocalDateTime publishedAt;

    @TableField("recycled_at")
    private LocalDateTime recycledAt;

    @TableField("request_id")
    private String requestId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
