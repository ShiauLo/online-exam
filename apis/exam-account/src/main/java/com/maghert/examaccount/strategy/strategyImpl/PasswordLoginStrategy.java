package com.maghert.examaccount.strategy.strategyImpl;

import cn.hutool.crypto.digest.BCrypt;
import com.maghert.examaccount.enums.LoginTypeEnum;
import com.maghert.examaccount.service.TokenService;
import com.maghert.examaccount.service.support.AccountLookupService;
import com.maghert.examaccount.strategy.LoginStrategy;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.exception.NotNullException;
import com.maghert.examcommon.exception.PasswordNotEqualException;
import com.maghert.examcommon.exception.UserNotExistsException;
import com.maghert.examcommon.pojo.dto.LoginDTO;
import com.maghert.examcommon.pojo.entity.SysUser;
import com.maghert.examcommon.pojo.vo.LoginVO;
import com.maghert.examcommon.web.ApiResponse;
import org.springframework.stereotype.Component;

@Component
public class PasswordLoginStrategy implements LoginStrategy {

    private final AccountLookupService accountLookupService;
    private final TokenService tokenService;

    public PasswordLoginStrategy(AccountLookupService accountLookupService, TokenService tokenService) {
        this.accountLookupService = accountLookupService;
        this.tokenService = tokenService;
    }

    @Override
    public ApiResponse<LoginVO> login(LoginDTO loginDTO)
            throws NotNullException, UserNotExistsException, PasswordNotEqualException, BusinessException {
        if (loginDTO.getAccount() == null || loginDTO.getPassword() == null) {
            throw new NotNullException("账号/密码不能为空");
        }

        SysUser user = accountLookupService.findByLoginAccount(loginDTO.getAccount());
        if (user == null) {
            throw new UserNotExistsException();
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "账户已被冻结");
        }

        if (user.getPassword() == null || !BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
            throw new PasswordNotEqualException();
        }

        return tokenService.generateAccessTokenAndRefreshToken(user);
    }

    @Override
    public LoginTypeEnum supportType() {
        return LoginTypeEnum.PASSWORD_LOGIN;
    }
}
