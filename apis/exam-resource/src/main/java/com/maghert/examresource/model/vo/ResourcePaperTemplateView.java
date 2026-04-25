package com.maghert.examresource.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResourcePaperTemplateView {

    private String paperType;
    private String fileName;
    private String contentType;
    private String downloadPath;
    private Long size;
}
