package com.maghert.examscore.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ScoreApplyRecheckView {

    private Long appealId;
    private Long scoreId;
    private Long questionId;
    private String status;
    private String recheckStatus;
    private LocalDateTime createdAt;
}
