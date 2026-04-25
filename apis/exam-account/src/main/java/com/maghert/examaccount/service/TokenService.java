package com.maghert.examaccount.service;

import com.maghert.examcommon.pojo.entity.SysUser;
import com.maghert.examcommon.pojo.vo.LoginVO;
import com.maghert.examcommon.web.ApiResponse;

public interface TokenService {

    ApiResponse<LoginVO> generateAccessTokenAndRefreshToken(SysUser user);

    ApiResponse<String> refreshToken(String refreshToken);
}
