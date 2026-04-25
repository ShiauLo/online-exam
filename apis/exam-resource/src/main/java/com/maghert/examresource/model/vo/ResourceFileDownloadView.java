package com.maghert.examresource.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResourceFileDownloadView {

    private String fileKey;
    private String fileName;
    private String contentType;
    private String downloadPath;
    private Long size;
}
