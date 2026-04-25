package com.maghert.examscore.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ScoreHandleAppealView {

    private Long appealId;
    private Long scoreId;
    private Long questionId;
    private String status;
    private String recheckStatus;
    private String handleResult;
    private String scoreStatus;
    private LocalDateTime handledAt;
}
