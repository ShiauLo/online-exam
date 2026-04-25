package com.maghert.examsystem.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AlarmSettingVO {
    private Long alarmId;
    private String alarmType;
    private String threshold;
    private List<String> recipients;
}
