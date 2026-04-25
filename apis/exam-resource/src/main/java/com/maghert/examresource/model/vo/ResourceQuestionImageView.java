package com.maghert.examresource.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResourceQuestionImageView {

    private String imgKey;
    private String fileName;
    private String contentType;
    private String accessPath;
    private Long size;
}
