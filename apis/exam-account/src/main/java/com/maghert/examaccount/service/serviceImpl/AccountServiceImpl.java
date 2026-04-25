package com.maghert.examaccount.service.serviceImpl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.crypto.digest.BCrypt;
import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.teautil.models.RuntimeOptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maghert.examaccount.config.AliyunSmsProperties;
import com.maghert.examaccount.mapper.AccountMapper;
import com.maghert.examaccount.pojo.dto.AccountAuditDTO;
import com.maghert.examaccount.service.AccountService;
import com.maghert.examaccount.service.support.AccountStatusLogService;
import com.maghert.examaccount.service.support.VerifyCodeService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.maghert.examaccount.constants.AccountConstants.OK;
import static com.maghert.examaccount.constants.AccountConstants.REDIS_ACCESS_TOKEN;
import static com.maghert.examaccount.constants.AccountConstants.REDIS_REFRESH_TOKEN;

@Slf4j
@Service
public class AccountServiceImpl extends ServiceImpl<AccountMapper, SysUser> implements AccountService {

    private final AliyunSmsProperties smsProperties;
    private final Client client;
    private final AccountMapper accountMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final Snowflake snowflake;
    private final VerifyCodeService verifyCodeService;
    private final AccountStatusLogService accountStatusLogService;

    public AccountServiceImpl(
            AliyunSmsProperties smsProperties,
            AccountMapper accountMapper,
            RedisTemplate<String, String> redisTemplate,
            Client client,
            Snowflake snowflake,
            VerifyCodeService verifyCodeService,
            AccountStatusLogService accountStatusLogService) {
        this.smsProperties = smsProperties;
        this.accountMapper = accountMapper;
        this.redisTemplate = redisTemplate;
        this.client = client;
        this.snowflake = snowflake;
        this.verifyCodeService = verifyCodeService;
        this.accountStatusLogService = accountStatusLogService;
    }

    @Override
    public ApiResponse<String> create(AccountCreateDTO accountCreateDTO, Long userId) throws AccountCreateException {
        userId = userId != null ? userId : snowflake.nextId();
        SysUser sysUser = BeanUtil.copyProperties(accountCreateDTO, SysUser.class).setId(userId);
        sysUser.setUsername(resolveUsername(accountCreateDTO, userId));
        sysUser.setRealName(resolveRealName(accountCreateDTO, sysUser.getUsername()));
        sysUser.setRoleId(resolveCreateRoleId(accountCreateDTO));
        sysUser.setPhoneNumber(normalizePhone(accountCreateDTO.getPhoneNumber()));
        sysUser.setStatus(resolveStatusValue(true));
        sysUser.setPassword(hashPassword(accountCreateDTO.getPassword()));

        try {
            if (accountMapper.insert(sysUser) != 1) {
                throw new AccountCreateException();
            }
        } catch (AccountCreateException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建账号失败，username={}", sysUser.getUsername(), e);
            throw new AccountCreateException();
        }
        return ApiResponse.ok();
    }

    @Override
    public ApiResponse<String> sendVerifyCode(SendVerifyCodeDTO sendVerifyCodeDTO) throws SendCodeException {
        String verifyCode = verifyCodeService.generateCode();
        SendSmsVerifyCodeRequest sendSmsRequest = new SendSmsVerifyCodeRequest()
                .setPhoneNumber(sendVerifyCodeDTO.getPhoneNumber())
                .setSignName(smsProperties.getSignName())
                .setTemplateCode(smsProperties.getTemplateCode())
                .setTemplateParam(verifyCodeService.renderSmsTemplate(verifyCode));
        try {
            SendSmsVerifyCodeResponse resp = client.sendSmsVerifyCodeWithOptions(sendSmsRequest, new RuntimeOptions());
            if (resp == null || resp.getBody() == null || !OK.equals(resp.getBody().getMessage())) {
                throw verifyCodeService.sendFailure();
            }
            verifyCodeService.cacheLoginCode(sendVerifyCodeDTO.getPhoneNumber(), verifyCode);
        } catch (SendCodeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to send verify code to phone {}", sendVerifyCodeDTO.getPhoneNumber(), e);
            throw verifyCodeService.sendFailure();
        }
        return ApiResponse.ok();
    }

    @Override
    public ApiResponse<PageResult<AccountQueryVO>> query(AccountQueryDTO accountQueryDTO) {
        long pageNum = Math.max(1L, accountQueryDTO.getPageNum());
        long pageSize = Math.max(1L, accountQueryDTO.getPageSize());
        long offset = (pageNum - 1) * pageSize;
        Integer roleId = resolveRoleId(accountQueryDTO.getRoleId(), accountQueryDTO.getRoleType());
        if (StringUtils.hasText(accountQueryDTO.getRoleType()) && roleId == null) {
            return ApiResponse.ok(new PageResult<>(List.of(), 0, (int) pageNum, (int) pageSize));
        }

        LambdaQueryWrapper<SysUser> queryWrapper = buildAccountQueryWrapper(accountQueryDTO, roleId)
                .orderByDesc(SysUser::getCreateTime)
                .last("LIMIT " + offset + "," + pageSize);
        long total = accountMapper.selectCount(buildAccountQueryWrapper(accountQueryDTO, roleId));

        List<SysUser> users = accountMapper.selectList(queryWrapper);
        List<AccountQueryVO> result = new ArrayList<>(users.size());
        for (SysUser user : users) {
            result.add(toAccountQueryVO(user));
        }
        return ApiResponse.ok(new PageResult<>(result, total, (int) pageNum, (int) pageSize));
    }

    @Override
    public ApiResponse<String> audit(AccountAuditDTO accountAuditDTO) throws UserNotExistsException, UpdateMYSQLException {
        SysUser sysUser = getById(accountAuditDTO.getId());
        if (sysUser == null) {
            throw new UserNotExistsException();
        }

        Integer targetStatus = resolveAuditStatus(accountAuditDTO.getAuditResult());
        LambdaUpdateWrapper<SysUser> updateWrapper = new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, accountAuditDTO.getId())
                .set(SysUser::getStatus, targetStatus);
        if (!update(updateWrapper)) {
            throw new UpdateMYSQLException();
        }

        return ApiResponse.ok();
    }

    @Override
    public ApiResponse<String> freeze(AccountFreezeDTO accountFreezeDTO) throws UserNotExistsException, UpdateMYSQLException {
        SysUser sysUser = getById(accountFreezeDTO.getId());
        if (sysUser == null) {
            throw new UserNotExistsException();
        }

        LambdaUpdateWrapper<SysUser> updateWrapper = new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, accountFreezeDTO.getId())
                .set(SysUser::getStatus, resolveStatusValue(!Boolean.TRUE.equals(accountFreezeDTO.getIsFrozen())));
        if (!update(updateWrapper)) {
            throw new UpdateMYSQLException();
        }

        accountStatusLogService.recordFreezeStatusChange(sysUser, accountFreezeDTO);
        return ApiResponse.ok();
    }

    @Override
    public ApiResponse<String> update(AccountUpdateDTO accountUpdateDTO) throws UserNotExistsException, UpdateMYSQLException {
        SysUser sysUser = getById(accountUpdateDTO.getUserId());
        if (sysUser == null) {
            throw new UserNotExistsException();
        }

        BeanUtil.copyProperties(accountUpdateDTO, sysUser, CopyOptions.create().ignoreNullValue());
        if (StringUtils.hasText(accountUpdateDTO.getRoleType()) || accountUpdateDTO.getRoleId() != null) {
            sysUser.setRoleId(resolveRoleId(accountUpdateDTO.getRoleId(), accountUpdateDTO.getRoleType()));
        }
        if (accountUpdateDTO.getPhoneNumber() != null) {
            sysUser.setPhoneNumber(normalizePhone(accountUpdateDTO.getPhoneNumber()));
        }
        if (!updateById(sysUser)) {
            throw new UpdateMYSQLException();
        }

        return ApiResponse.ok();
    }

    @Override
    public ApiResponse<String> delete(AccountDeleteDTO accountDeleteDTO) throws UpdateMYSQLException {
        if (!removeById(accountDeleteDTO.getId())) {
            throw new UpdateMYSQLException();
        }
        return ApiResponse.ok();
    }

    @Override
    public ApiResponse<String> resetPassword(AccountPasswordResetDTO accountPasswordResetDTO)
            throws UserNotExistsException, UpdateMYSQLException {
        SysUser sysUser = getById(accountPasswordResetDTO.getId());
        if (sysUser == null) {
            throw new UserNotExistsException();
        }

        sysUser.setPassword(BCrypt.hashpw(accountPasswordResetDTO.getNewPassword()));
        if (!updateById(sysUser)) {
            throw new UpdateMYSQLException();
        }

        return ApiResponse.ok();
    }

    @Override
    public ApiResponse<String> logout(AccountLogOutDTO accountLogOutDTO) throws UpdateRedisException {
        try {
            redisTemplate.delete(List.of(
                    REDIS_REFRESH_TOKEN + accountLogOutDTO.getUserId(),
                    REDIS_ACCESS_TOKEN + accountLogOutDTO.getUserId()
            ));
        } catch (Exception e) {
            log.error("退出登录清理令牌失败，userId={}", accountLogOutDTO.getUserId(), e);
            throw new UpdateRedisException();
        }
        return ApiResponse.ok();
    }

    private LambdaQueryWrapper<SysUser> buildAccountQueryWrapper(AccountQueryDTO accountQueryDTO, Integer roleId) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<SysUser>()
                .eq(accountQueryDTO.getUserId() != null, SysUser::getId, accountQueryDTO.getUserId())
                .eq(roleId != null, SysUser::getRoleId, roleId);
        if (StringUtils.hasText(accountQueryDTO.getKeyword())) {
            String keyword = accountQueryDTO.getKeyword().trim();
            queryWrapper.and(wrapper -> wrapper
                    .like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getRealName, keyword)
                    .or()
                    .like(SysUser::getPhoneNumber, keyword)
                    .or()
                    .like(SysUser::getEmail, keyword));
        }
        return queryWrapper;
    }

    private AccountQueryVO toAccountQueryVO(SysUser user) {
        return new AccountQueryVO(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                resolveRoleType(user.getRoleId()),
                normalizePhone(user.getPhoneNumber()),
                user.getEmail(),
                resolveStatus(user.getStatus())
        );
    }

    private String resolveUsername(AccountCreateDTO accountCreateDTO, Long userId) {
        if (StringUtils.hasText(accountCreateDTO.getUsername())) {
            return accountCreateDTO.getUsername().trim();
        }
        String phoneNumber = normalizePhone(accountCreateDTO.getPhoneNumber());
        if (StringUtils.hasText(phoneNumber) && phoneNumber.length() >= 4) {
            return "student_" + phoneNumber.substring(phoneNumber.length() - 4);
        }
        return "user_" + userId;
    }

    private String resolveRealName(AccountCreateDTO accountCreateDTO, String username) {
        if (StringUtils.hasText(accountCreateDTO.getRealName())) {
            return accountCreateDTO.getRealName().trim();
        }
        return username;
    }

    private String hashPassword(String rawPassword) {
        String targetPassword = StringUtils.hasText(rawPassword)
                ? rawPassword.trim()
                : "System@" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return BCrypt.hashpw(targetPassword);
    }

    private String normalizePhone(String phoneNumber) {
        return phoneNumber == null ? "" : phoneNumber.trim();
    }

    private Integer resolveCreateRoleId(AccountCreateDTO accountCreateDTO) {
        Integer roleId = resolveRoleId(accountCreateDTO.getRoleId(), accountCreateDTO.getRoleType());
        return roleId != null ? roleId : 4;
    }

    private Integer resolveRoleId(Integer roleId, String roleType) {
        if (roleId != null) {
            return roleId;
        }
        if (!StringUtils.hasText(roleType)) {
            return null;
        }
        return switch (roleType.trim().toLowerCase()) {
            case "super_admin" -> 1;
            case "admin" -> 2;
            case "teacher" -> 3;
            case "student" -> 4;
            case "auditor" -> 5;
            case "operator", "ops" -> 6;
            default -> null;
        };
    }

    private String resolveRoleType(Integer roleId) {
        if (roleId == null) {
            return null;
        }
        return switch (roleId) {
            case 1 -> "super_admin";
            case 2 -> "admin";
            case 3 -> "teacher";
            case 4 -> "student";
            case 5 -> "auditor";
            case 6 -> "ops";
            default -> "unknown";
        };
    }

    private Integer resolveStatusValue(boolean active) {
        return active ? 1 : 0;
    }

    private String resolveStatus(Integer status) {
        return status != null && status == 1 ? "active" : "frozen";
    }

    private Integer resolveAuditStatus(String auditResult) {
        return "approve".equalsIgnoreCase(auditResult) || "approved".equalsIgnoreCase(auditResult)
                ? resolveStatusValue(true)
                : resolveStatusValue(false);
    }
}
