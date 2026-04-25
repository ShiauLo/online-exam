package com.maghert.examscore.model.dto;

import com.maghert.examcommon.pojo.query.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ScoreQueryRequest extends PageQuery {

    private Long examId;
    private Long classId;
    private Long studentId;
    private String status;
    private String keyword;
}
