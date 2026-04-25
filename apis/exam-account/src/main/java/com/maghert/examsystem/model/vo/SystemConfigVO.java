package com.maghert.examsystem.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SystemConfigVO {
    private Long configId;
    private String configKey;
    private String configValue;
    private String category;
}
