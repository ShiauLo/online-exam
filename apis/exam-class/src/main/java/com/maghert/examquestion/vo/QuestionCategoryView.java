package com.maghert.examquestion.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuestionCategoryView {

    private Long categoryId;
    private String name;
    private Long parentId;
    private Boolean isPersonal;
    private Boolean isDisabled;
    private Long ownerId;
}
