package com.maghert.examcore.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ExamView {

    private Long examId;
    private String examName;
    private Long paperId;
    private String paperName;
    private String status;
    private Long creatorId;
    private Integer duration;
    private LocalDateTime startTime;
    private List<Long> classIds;
}
