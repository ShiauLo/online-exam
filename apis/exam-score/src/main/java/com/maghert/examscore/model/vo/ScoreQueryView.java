package com.maghert.examscore.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ScoreQueryView {

    private Long scoreId;
    private Long examId;
    private String examName;
    private Long classId;
    private String className;
    private Long studentId;
    private String studentName;
    private Integer totalScore;
    private Integer objectiveScore;
    private Integer subjectiveScore;
    private String status;
    private String publishStatus;
    private Long appealId;
    private String recheckStatus;
    private LocalDateTime submittedAt;
    private LocalDateTime publishedAt;
}
