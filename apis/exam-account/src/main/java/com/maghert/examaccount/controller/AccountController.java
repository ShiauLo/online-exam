package com.maghert.examaccount.controller;

import com.maghert.examaccount.pojo.dto.AccountAuditDTO;
import com.maghert.examaccount.service.AccountService;
import com.maghert.examaccount.service.LoginService;
import com.maghert.examaccount.service.TokenService;
import com.maghert.examcommon.exception.AccountCreateException;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.exception.SendCodeException;
import com.maghert.examcommon.exception.UpdateMYSQLException;
import com.maghert.examcommon.exception.UpdateRedisException;
import com.maghert.examcommon.exception.UserNotExistsException;
import com.maghert.examcommon.pojo.dto.AccountCreateDTO;
import com.maghert.examcommon.pojo.dto.AccountDeleteDTO;
import com.maghert.examcommon.pojo.dto.AccountFreezeDTO;
import com.maghert.examcommon.pojo.dto.AccountLogOutDTO;
import com.maghert.examcommon.pojo.dto.AccountPasswordResetDTO;
import com.maghert.examcommon.pojo.dto.AccountQueryDTO;
import com.maghert.examcommon.pojo.dto.AccountUpdateDTO;
import com.maghert.examcommon.pojo.dto.RefreshTokenDTO;
import com.maghert.examcommon.pojo.dto.LoginDTO;
import com.maghert.examcommon.pojo.dto.SendVerifyCodeDTO;
import com.maghert.examcommon.pojo.vo.AccountQueryVO;
import com.maghert.examcommon.pojo.vo.LoginVO;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examcommon.web.PageResult;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;
    private final LoginService loginService;
    private final TokenService tokenService;

    public AccountController(AccountService accountService, LoginService loginService, TokenService tokenService) {
        this.accountService = accountService;
        this.loginService = loginService;
        this.tokenService = tokenService;
    }

    @PostMapping("/create")
    public ApiResponse<String> accountCreate(@RequestBody @Valid AccountCreateDTO accountCreateDTO)
            throws AccountCreateException {
        return accountService.create(accountCreateDTO, null);
    }

    @PostMapping("/login")
    public ApiResponse<LoginVO> accountLogin(@RequestBody @Valid LoginDTO loginDTO) throws BusinessException {
        return loginService.login(loginDTO);
    }

    @PostMapping("/refresh-token")
    public ApiResponse<String> accountRefreshToken(@RequestBody @Valid RefreshTokenDTO refreshTokenDTO) {
        return tokenService.refreshToken(refreshTokenDTO.getRefreshToken());
    }

    @PostMapping("/send/verifycode")
    public ApiResponse<String> accountSendVerifyCode(@RequestBody @Valid SendVerifyCodeDTO sendVerifyCodeDTO)
            throws SendCodeException {
        return accountService.sendVerifyCode(sendVerifyCodeDTO);
    }

    @PostMapping("/query")
    public ApiResponse<PageResult<AccountQueryVO>> accountQuery(@RequestBody @Valid AccountQueryDTO accountQueryDTO) {
        return accountService.query(accountQueryDTO);
    }

    @PutMapping("/audit")
    public ApiResponse<String> accountAudit(@RequestBody @Valid AccountAuditDTO accountAuditDTO)
            throws UserNotExistsException, UpdateMYSQLException {
        return accountService.audit(accountAuditDTO);
    }

    @PostMapping("/freeze")
    public ApiResponse<String> accountFreeze(@RequestBody @Valid AccountFreezeDTO accountFreezeDTO)
            throws UserNotExistsException, UpdateMYSQLException {
        return accountService.freeze(accountFreezeDTO);
    }

    @PutMapping("/update")
    public ApiResponse<String> accountUpdate(@RequestBody @Valid AccountUpdateDTO accountUpdateDTO)
            throws UserNotExistsException, UpdateMYSQLException {
        return accountService.update(accountUpdateDTO);
    }

    @DeleteMapping("/delete")
    public ApiResponse<String> accountDelete(@RequestBody @Valid AccountDeleteDTO accountDeleteDTO)
            throws UpdateMYSQLException {
        return accountService.delete(accountDeleteDTO);
    }

    @PutMapping("/reset-password")
    public ApiResponse<String> accountPasswordReset(@RequestBody @Valid AccountPasswordResetDTO accountPasswordResetDTO)
            throws UserNotExistsException, UpdateMYSQLException {
        return accountService.resetPassword(accountPasswordResetDTO);
    }

    @PostMapping("/logout")
    public ApiResponse<String> accountLogOut(@RequestBody @Valid AccountLogOutDTO accountLogOutDTO)
            throws UpdateRedisException {
        return accountService.logout(accountLogOutDTO);
    }
}
