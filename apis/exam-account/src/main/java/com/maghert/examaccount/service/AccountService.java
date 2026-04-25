package com.maghert.examaccount.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maghert.examaccount.pojo.dto.AccountAuditDTO;
import com.maghert.examcommon.exception.AccountCreateException;
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
import com.maghert.examcommon.pojo.dto.SendVerifyCodeDTO;
import com.maghert.examcommon.pojo.entity.SysUser;
import com.maghert.examcommon.pojo.vo.AccountQueryVO;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examcommon.web.PageResult;
import jakarta.validation.Valid;

public interface AccountService extends IService<SysUser> {

    ApiResponse<String> create(AccountCreateDTO accountCreateDTO, Long userId) throws AccountCreateException;

    ApiResponse<String> sendVerifyCode(@Valid SendVerifyCodeDTO sendVerifyCodeDTO) throws SendCodeException;

    ApiResponse<PageResult<AccountQueryVO>> query(AccountQueryDTO accountQueryDTO);

    ApiResponse<String> audit(@Valid AccountAuditDTO accountAuditDTO) throws UserNotExistsException, UpdateMYSQLException;

    ApiResponse<String> freeze(@Valid AccountFreezeDTO accountFreezeDTO) throws UserNotExistsException, UpdateMYSQLException;

    ApiResponse<String> update(@Valid AccountUpdateDTO accountUpdateDTO) throws UserNotExistsException, UpdateMYSQLException;

    ApiResponse<String> delete(@Valid AccountDeleteDTO accountDeleteDTO) throws UpdateMYSQLException;

    ApiResponse<String> resetPassword(@Valid AccountPasswordResetDTO accountPasswordResetDTO)
            throws UserNotExistsException, UpdateMYSQLException;

    ApiResponse<String> logout(@Valid AccountLogOutDTO accountLogOutDTO) throws UpdateRedisException;
}
