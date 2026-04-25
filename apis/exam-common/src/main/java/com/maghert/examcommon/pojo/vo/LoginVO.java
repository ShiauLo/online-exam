package com.maghert.examcommon.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginVO {

    private String accessToken;
    private String refreshToken;
}
