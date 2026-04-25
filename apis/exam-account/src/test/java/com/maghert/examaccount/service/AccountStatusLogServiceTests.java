package com.maghert.examaccount.service;

import cn.hutool.core.lang.Snowflake;
import com.maghert.examaccount.mapper.AccountStatusLogMapper;
import com.maghert.examaccount.service.support.AccountStatusLogService;
import com.maghert.examcommon.pojo.dto.AccountFreezeDTO;
import com.maghert.examcommon.pojo.entity.SysAccountStatusLog;
import com.maghert.examcommon.pojo.entity.SysUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountStatusLogServiceTests {

    @Mock
    private AccountStatusLogMapper accountStatusLogMapper;

    @Mock
    private Snowflake snowflake;

    @Test
    void shouldFillOperateTimeAndDefaultReasonWhenRecordingFreezeStatusChange() throws Exception {
        AccountStatusLogService service = new AccountStatusLogService(accountStatusLogMapper, snowflake);
        SysUser sysUser = new SysUser()
                .setId(1001L)
                .setStatus(1)
                .setPhoneNumber("13812345678");
        AccountFreezeDTO dto = new AccountFreezeDTO(1001L, true, null);
        when(snowflake.nextId()).thenReturn(9001L);
        when(accountStatusLogMapper.insert(any(SysAccountStatusLog.class))).thenReturn(1);

        service.recordFreezeStatusChange(sysUser, dto);

        ArgumentCaptor<SysAccountStatusLog> captor = ArgumentCaptor.forClass(SysAccountStatusLog.class);
        verify(accountStatusLogMapper).insert(captor.capture());
        SysAccountStatusLog logEntity = captor.getValue();
        assertEquals(9001L, logEntity.getId());
        assertEquals(1, logEntity.getOperateType());
        assertEquals("管理员执行冻结操作", logEntity.getOperateReason());
        assertEquals("138****5678", logEntity.getTargetUserPhone());
        assertNotNull(logEntity.getOperateTime());
    }
}
