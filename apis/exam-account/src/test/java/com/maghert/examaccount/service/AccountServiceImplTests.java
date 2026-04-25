package com.maghert.examaccount.service;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.crypto.digest.BCrypt;
import com.aliyun.dypnsapi20170525.Client;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.maghert.examaccount.pojo.dto.AccountAuditDTO;
import com.maghert.examaccount.config.AliyunSmsProperties;
import com.maghert.examaccount.mapper.AccountMapper;
import com.maghert.examaccount.service.serviceImpl.AccountServiceImpl;
import com.maghert.examaccount.service.support.AccountStatusLogService;
import com.maghert.examaccount.service.support.VerifyCodeService;
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
import com.maghert.examcommon.pojo.entity.SysUser;
import com.maghert.examcommon.pojo.vo.AccountQueryVO;
import com.maghert.examcommon.web.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

import static com.maghert.examaccount.constants.AccountConstants.REDIS_ACCESS_TOKEN;
import static com.maghert.examaccount.constants.AccountConstants.REDIS_REFRESH_TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTests {

    @Mock
    private AliyunSmsProperties smsProperties;
    @Mock
    private AccountMapper accountMapper;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private Client client;
    @Mock
    private Snowflake snowflake;
    @Mock
    private VerifyCodeService verifyCodeService;
    @Mock
    private AccountStatusLogService accountStatusLogService;

    private AccountServiceImpl service;

    @BeforeEach
    void setUp() {
        initializeTableInfo();
        service = spy(new AccountServiceImpl(
                smsProperties,
                accountMapper,
                redisTemplate,
                client,
                snowflake,
                verifyCodeService,
                accountStatusLogService));
    }

    private void initializeTableInfo() {
        if (TableInfoHelper.getTableInfo(SysUser.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                    SysUser.class);
        }
    }

    @Test
    void shouldHashPasswordWhenCreatingAccount() throws Exception {
        AccountCreateDTO dto = new AccountCreateDTO();
        dto.setUsername("alice");
        dto.setPassword("PlainText#123");
        dto.setPhoneNumber("13812345678");
        when(accountMapper.insert(any(SysUser.class))).thenReturn(1);

        service.create(dto, 1001L);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(accountMapper).insert(captor.capture());
        SysUser created = captor.getValue();
        assertNotEquals("PlainText#123", created.getPassword());
        assertTrue(BCrypt.checkpw("PlainText#123", created.getPassword()));
        assertEquals(1001L, created.getId());
    }

    @Test
    void shouldMaskPhoneAndResolveRoleWhenQueryingAccounts() {
        AccountQueryDTO dto = new AccountQueryDTO(null, null, null, null, null, 1L, 10L);
        SysUser user = new SysUser()
                .setId(1001L)
                .setUsername("alice")
                .setRealName("Alice")
                .setRoleId(3)
                .setPhoneNumber("13812345678")
                .setEmail("alice@example.com")
                .setStatus(1);
        when(accountMapper.selectList(any())).thenReturn(List.of(user));
        when(accountMapper.selectCount(any())).thenReturn(1L);

        PageResult<AccountQueryVO> result = service.query(dto).getData();

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals(1001L, result.getRecords().get(0).getAccountId());
        assertEquals("13812345678", result.getRecords().get(0).getPhone());
        assertEquals("teacher", result.getRecords().get(0).getRoleType());
        assertEquals("Alice", result.getRecords().get(0).getRealName());
        assertEquals("alice@example.com", result.getRecords().get(0).getEmail());
        assertEquals("active", result.getRecords().get(0).getStatus());
    }

    @Test
    void shouldActivateAccountWhenAuditApproved() throws Exception {
        AccountAuditDTO dto = new AccountAuditDTO(1L, "approve");
        SysUser user = new SysUser().setId(1L).setStatus(0);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaUpdateWrapper<SysUser>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        doReturn(user).when(service).getById(1L);
        doReturn(true).when(service).update(captor.capture());

        service.audit(dto);

        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(1));
    }

    @Test
    void shouldThrow404WhenFreezingMissingUser() {
        AccountFreezeDTO dto = new AccountFreezeDTO(1L, true, "reason");
        doReturn(null).when(service).getById(1L);

        UserNotExistsException exception = assertThrows(UserNotExistsException.class, () -> service.freeze(dto));

        assertEquals(404, exception.getCode());
    }

    @Test
    void shouldThrow500WhenFreezeUpdateFails() {
        AccountFreezeDTO dto = new AccountFreezeDTO(1L, true, "reason");
        SysUser user = new SysUser().setId(1L).setStatus(0).setPhoneNumber("13812345678");
        doReturn(user).when(service).getById(1L);
        doReturn(false).when(service).update(org.mockito.ArgumentMatchers.<LambdaUpdateWrapper<SysUser>>any());

        UpdateMYSQLException exception = assertThrows(UpdateMYSQLException.class, () -> service.freeze(dto));

        assertEquals(500, exception.getCode());
    }

    @Test
    void shouldRecordFreezeStatusChangeWhenFreezingSucceeds() throws Exception {
        AccountFreezeDTO dto = new AccountFreezeDTO(1L, true, "reason");
        SysUser user = new SysUser().setId(1L).setStatus(1).setPhoneNumber("13812345678");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaUpdateWrapper<SysUser>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        doReturn(user).when(service).getById(1L);
        doReturn(true).when(service).update(captor.capture());

        service.freeze(dto);

        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(0));
        verify(accountStatusLogService).recordFreezeStatusChange(user, dto);
    }

    @Test
    void shouldThrow404WhenUpdatingMissingUser() {
        AccountUpdateDTO dto = new AccountUpdateDTO();
        dto.setUserId(1L);
        doReturn(null).when(service).getById(1L);

        UserNotExistsException exception = assertThrows(UserNotExistsException.class, () -> service.update(dto));

        assertEquals(404, exception.getCode());
    }

    @Test
    void shouldThrow500WhenDeleteFails() {
        doReturn(false).when(service).removeById(anyLong());

        UpdateMYSQLException exception = assertThrows(UpdateMYSQLException.class,
                () -> service.delete(new AccountDeleteDTO(1L)));

        assertEquals(500, exception.getCode());
    }

    @Test
    void shouldHashPasswordWhenResettingPassword() throws Exception {
        SysUser user = new SysUser().setId(1L).setPassword("old");
        AccountPasswordResetDTO dto = new AccountPasswordResetDTO(1L, "NewPassword#1", "123456");
        doReturn(user).when(service).getById(1L);
        doReturn(true).when(service).updateById(any(SysUser.class));

        service.resetPassword(dto);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(service).updateById(captor.capture());
        assertTrue(BCrypt.checkpw("NewPassword#1", captor.getValue().getPassword()));
    }

    @Test
    void shouldAllowLogoutWhenTokensAreMissing() throws Exception {
        service.logout(new AccountLogOutDTO(1L, "refresh-token"));

        verify(redisTemplate).delete(List.of(
                REDIS_REFRESH_TOKEN + 1L,
                REDIS_ACCESS_TOKEN + 1L
        ));
    }
}
