package com.maghert.examcommon.pojo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountQueryDTO {

    @JsonAlias("accountId")
    private Long userId;

    private String classId;

    private String keyword;

    private Integer roleId;

    private String roleType;

    @NotNull
    private Long pageNum;

    @NotNull
    private Long pageSize;

}
