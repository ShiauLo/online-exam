package com.maghert.examscore.model.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ScoreAnalyzeView {

    private Long examId;
    private String examName;
    private Long classId;
    private Integer passScore;
    private Integer totalScore;
    private Integer totalParticipants;
    private Integer finishedParticipants;
    private LocalDateTime generatedAt;
    private Overview overview;
    private List<StatusDistributionItem> statusDistribution;
    private List<ScoreRangeDistributionItem> scoreRangeDistribution;
    private List<ClassDistributionItem> classDistribution;

    @Data
    @Builder
    public static class Overview {

        private BigDecimal averageScore;
        private Integer highestScore;
        private Integer lowestScore;
        private Integer passCount;
        private BigDecimal passRate;
    }

    @Data
    @Builder
    public static class StatusDistributionItem {

        private String status;
        private Integer count;
    }

    @Data
    @Builder
    public static class ScoreRangeDistributionItem {

        private String rangeLabel;
        private Integer count;
    }

    @Data
    @Builder
    public static class ClassDistributionItem {

        private Long classId;
        private String className;
        private Integer studentCount;
        private Integer finishedCount;
        private BigDecimal averageScore;
        private BigDecimal passRate;
    }
}
