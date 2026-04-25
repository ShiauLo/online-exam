package com.maghert.examcore.entity;

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
@TableName("exam_instance")
public class ExamEntity {

    @TableId(value = "exam_id", type = IdType.INPUT)
    private Long examId;

    @TableField("exam_name")
    private String examName;

    @TableField("paper_id")
    private Long paperId;

    @TableField("paper_name")
    private String paperName;

    private String status;

    @TableField("creator_id")
    private Long creatorId;

    @TableField("creator_role_id")
    private Integer creatorRoleId;

    private Integer duration;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("request_id")
    private String requestId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
