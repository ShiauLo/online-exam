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
public class AccountFreezeDTO {

    @NotNull
    @JsonAlias("accountId")
    private Long id;

    @NotNull
    private Boolean isFrozen;

    private String reason;
}
