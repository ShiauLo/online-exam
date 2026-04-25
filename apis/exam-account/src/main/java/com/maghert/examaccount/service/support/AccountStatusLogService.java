package com.maghert.examaccount.service.support;

import cn.hutool.core.lang.Snowflake;
import com.maghert.examaccount.mapper.AccountStatusLogMapper;
import com.maghert.examcommon.exception.UpdateMYSQLException;
import com.maghert.examcommon.pojo.dto.AccountFreezeDTO;
import com.maghert.examcommon.pojo.entity.SysAccountStatusLog;
import com.maghert.examcommon.pojo.entity.SysUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class AccountStatusLogService {

    private final AccountStatusLogMapper accountStatusLogMapper;
    private final Snowflake snowflake;

    public AccountStatusLogService(AccountStatusLogMapper accountStatusLogMapper, Snowflake snowflake) {
        this.accountStatusLogMapper = accountStatusLogMapper;
        this.snowflake = snowflake;
    }

    public void recordFreezeStatusChange(SysUser sysUser, AccountFreezeDTO accountFreezeDTO) throws UpdateMYSQLException {
        boolean frozen = Boolean.TRUE.equals(accountFreezeDTO.getIsFrozen());
        SysAccountStatusLog logEntity = new SysAccountStatusLog();
        logEntity.setId(snowflake.nextId());
        logEntity.setOperateUserId(0L);
        logEntity.setOperateUserName("system");
        logEntity.setOperateUserRole("SYSTEM");
        logEntity.setTargetUserId(sysUser.getId());
        logEntity.setTargetUserPhone(maskPhone(sysUser.getPhoneNumber()));
        logEntity.setOperateType(frozen ? 1 : 2);
        logEntity.setOperateReason(resolveReason(accountFreezeDTO.getReason(), frozen));
        logEntity.setBeforeStatus(sysUser.getStatus());
        logEntity.setAfterStatus(frozen ? 0 : 1);
        logEntity.setOperateTime(LocalDateTime.now());

        if (accountStatusLogMapper.insert(logEntity) != 1) {
            throw new UpdateMYSQLException();
        }
    }

    private String maskPhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 7) {
            return phoneNumber;
        }
        return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }

    private String resolveReason(String reason, boolean frozen) {
        if (StringUtils.hasText(reason)) {
            return reason.trim();
        }
        return frozen ? "管理员执行冻结操作" : "管理员执行解冻操作";
    }
}
