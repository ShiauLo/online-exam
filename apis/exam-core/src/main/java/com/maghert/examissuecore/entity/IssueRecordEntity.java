package com.maghert.examissuecore.entity;

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
@TableName("exam_issue_record")
public class IssueRecordEntity {

    @TableId(value = "issue_id", type = IdType.INPUT)
    private Long issueId;

    private String type;

    private String title;

    @TableField("description")
    private String description;

    private String status;

    @TableField("reporter_id")
    private Long reporterId;

    @TableField("current_handler_id")
    private Long currentHandlerId;

    @TableField("exam_id")
    private Long examId;

    @TableField("class_id")
    private Long classId;

    @TableField("latest_result")
    private String latestResult;

    @TableField("latest_solution")
    private String latestSolution;

    @TableField("img_urls")
    private String imgUrls;

    @TableField("audit_trail")
    private String auditTrail;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
