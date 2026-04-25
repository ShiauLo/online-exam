package com.maghert.examclass.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@TableName("exam_class_import_record")
public class ExamClassImportRecordEntity {

    @TableId(value = "record_id", type = IdType.INPUT)
    private Long recordId;

    @TableField("file_name")
    private String fileName;

    @TableField("default_teacher_id")
    private Long defaultTeacherId;

    @TableField("imported_count")
    private Integer importedCount;

    @TableField("skipped_count")
    private Integer skippedCount;

    @TableField("operator_id")
    private Long operatorId;

    private String remark;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
