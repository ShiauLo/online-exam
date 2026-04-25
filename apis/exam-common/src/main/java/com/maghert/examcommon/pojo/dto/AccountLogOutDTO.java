package com.maghert.examcommon.pojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountLogOutDTO {

    @NotNull
    private Long userId;

    @NotNull
    private String refreshToken;

}
