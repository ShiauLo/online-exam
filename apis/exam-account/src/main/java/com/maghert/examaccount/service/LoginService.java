package com.maghert.examaccount.service;

import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.pojo.dto.LoginDTO;
import com.maghert.examcommon.pojo.vo.LoginVO;
import com.maghert.examcommon.web.ApiResponse;

public interface LoginService {

    ApiResponse<LoginVO> login(LoginDTO loginDTO) throws BusinessException;
}
