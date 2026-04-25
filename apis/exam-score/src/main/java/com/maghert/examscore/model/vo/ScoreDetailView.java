package com.maghert.examscore.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ScoreDetailView {

    private Long scoreId;
    private Long examId;
    private String examName;
    private Long paperId;
    private String paperName;
    private Long classId;
    private String className;
    private Long studentId;
    private String studentName;
    private Integer totalScore;
    private Integer objectiveScore;
    private Integer subjectiveScore;
    private String status;
    private LocalDateTime submittedAt;
    private LocalDateTime publishedAt;
    private List<ScoreQuestionDetailView> questionDetails;
}
