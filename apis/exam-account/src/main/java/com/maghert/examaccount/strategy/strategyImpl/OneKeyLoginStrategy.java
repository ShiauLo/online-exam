package com.maghert.examaccount.strategy.strategyImpl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Snowflake;
import com.maghert.examaccount.enums.LoginTypeEnum;
import com.maghert.examaccount.service.AccountService;
import com.maghert.examaccount.service.TokenService;
import com.maghert.examaccount.service.support.AccountLookupService;
import com.maghert.examaccount.service.support.VerifyCodeService;
import com.maghert.examaccount.strategy.LoginStrategy;
import com.maghert.examcommon.exception.AccountCreateException;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.exception.NotNullException;
import com.maghert.examcommon.exception.VerifyCodeNotEqualException;
import com.maghert.examcommon.pojo.dto.AccountCreateDTO;
import com.maghert.examcommon.pojo.dto.LoginDTO;
import com.maghert.examcommon.pojo.entity.SysUser;
import com.maghert.examcommon.pojo.vo.LoginVO;
import com.maghert.examcommon.web.ApiResponse;
import org.springframework.stereotype.Component;

@Component
public class OneKeyLoginStrategy implements LoginStrategy {

    private final AccountLookupService accountLookupService;
    private final AccountService accountService;
    private final VerifyCodeService verifyCodeService;
    private final Snowflake snowflake;
    private final TokenService tokenService;

    public OneKeyLoginStrategy(
            AccountLookupService accountLookupService,
            AccountService accountService,
            VerifyCodeService verifyCodeService,
            Snowflake snowflake,
            TokenService tokenService) {
        this.accountLookupService = accountLookupService;
        this.accountService = accountService;
        this.verifyCodeService = verifyCodeService;
        this.snowflake = snowflake;
        this.tokenService = tokenService;
    }

    @Override
    public ApiResponse<LoginVO> login(LoginDTO loginDTO)
            throws NotNullException, AccountCreateException, VerifyCodeNotEqualException, BusinessException {
        if (loginDTO.getPhone() == null || loginDTO.getVerifyCode() == null) {
            throw new NotNullException("手机号和验证码不能为空");
        }
        verifyCodeService.validateLoginCode(loginDTO.getPhone(), loginDTO.getVerifyCode());

        SysUser user = accountLookupService.findByPhone(loginDTO.getPhone());
        if (user != null && user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "账户已被冻结");
        }
        if (user == null) {
            Long userId = snowflake.nextId();
            AccountCreateDTO accountCreateDTO = BeanUtil.toBean(loginDTO, AccountCreateDTO.class);
            accountCreateDTO.setPhoneNumber(loginDTO.getPhone());
            accountCreateDTO.setUsername("student_" + userId);
            accountCreateDTO.setRealName("用户" + userId);
            accountCreateDTO.setRoleId(4);
            accountService.create(accountCreateDTO, userId);
            user = new SysUser()
                    .setId(userId)
                    .setPhoneNumber(accountCreateDTO.getPhoneNumber())
                    .setUsername(accountCreateDTO.getUsername())
                    .setRealName(accountCreateDTO.getRealName())
                    .setRoleId(accountCreateDTO.getRoleId())
                    .setStatus(1);
        }

        return tokenService.generateAccessTokenAndRefreshToken(user);
    }

    @Override
    public LoginTypeEnum supportType() {
        return LoginTypeEnum.ONE_KEY_LOGIN;
    }
}
