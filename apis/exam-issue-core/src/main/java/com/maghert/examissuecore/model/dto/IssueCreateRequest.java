package com.maghert.examissuecore.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class IssueCreateRequest {

    @NotBlank(message = "type 不能为空")
    private String type;

    @NotBlank(message = "title 不能为空")
    private String title;

    @NotBlank(message = "desc 不能为空")
    private String desc;

    private Long reporterId;

    private Long examId;

    private Long classId;

    private List<String> imgUrls;
}
