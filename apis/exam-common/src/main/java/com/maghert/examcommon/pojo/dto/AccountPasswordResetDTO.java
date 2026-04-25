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
public class AccountPasswordResetDTO {

    @NotNull
    @JsonAlias("accountId")
    private Long id;

    @NotNull
    private String newPassword;

    private String verifyCode;

}
