package com.maghert.examcommon.pojo.dto;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountDeleteDTO {

    @NotNull
    private Long id;

}
