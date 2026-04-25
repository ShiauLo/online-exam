package com.maghert.exampaper.model.dto;

import com.maghert.examcommon.pojo.query.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PaperQueryRequest extends PageQuery {

    private Long paperId;
    private Long creatorId;
    private Long classId;
    private String status;
    private String sourceType;
    private String keyword;
}
