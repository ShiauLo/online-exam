package com.maghert.examcore.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ExamSubmitRequest {

    @NotNull(message = "examId 不能为空")
    private Long examId;

    @NotNull(message = "studentId 不能为空")
    private Long studentId;

    @Valid
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {

        @NotNull(message = "questionId 不能为空")
        private Long questionId;

        private String answer;
    }
}
