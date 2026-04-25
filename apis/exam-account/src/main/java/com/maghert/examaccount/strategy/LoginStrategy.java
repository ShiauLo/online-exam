package com.maghert.examaccount.strategy;

import com.maghert.examaccount.enums.LoginTypeEnum;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.pojo.dto.LoginDTO;
import com.maghert.examcommon.pojo.vo.LoginVO;
import com.maghert.examcommon.web.ApiResponse;

public interface LoginStrategy {

    ApiResponse<LoginVO> login(LoginDTO loginDTO) throws BusinessException;

    LoginTypeEnum supportType();
}
