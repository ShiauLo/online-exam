package com.maghert.examcore.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ExamDistributeView {

    private Long examId;
    private String status;
    private List<Long> classIds;
    private List<Long> studentIds;
    private Integer distributedCount;
}
