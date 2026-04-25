package com.maghert.examquestion.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QuestionView {

    private Long questionId;
    private Long categoryId;
    private Long creatorId;
    private String content;
    private String type;
    private List<String> options;
    private String answer;
    private String analysis;
    private Integer difficulty;
    private String auditStatus;
    private Boolean isDisabled;
    private Integer referenceCount;
    private Boolean referenceLocked;
}
