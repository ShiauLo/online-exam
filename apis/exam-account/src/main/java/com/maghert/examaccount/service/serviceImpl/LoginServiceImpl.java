package com.maghert.examaccount.service.serviceImpl;

import com.maghert.examaccount.service.LoginService;
import com.maghert.examaccount.strategy.LoginStrategy;
import com.maghert.examaccount.strategy.strategyFactory.LoginStrategyFactory;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.pojo.dto.LoginDTO;
import com.maghert.examcommon.pojo.vo.LoginVO;
import com.maghert.examcommon.web.ApiResponse;
import org.springframework.stereotype.Service;

@Service
public class LoginServiceImpl implements LoginService {

    private final LoginStrategyFactory loginStrategyFactory;

    public LoginServiceImpl(LoginStrategyFactory loginStrategyFactory) {
        this.loginStrategyFactory = loginStrategyFactory;
    }

    @Override
    public ApiResponse<LoginVO> login(LoginDTO loginDTO) throws BusinessException {
        LoginStrategy strategy = loginStrategyFactory.getStrategy(loginDTO.getLoginType());
        return strategy.login(loginDTO);
    }
}
