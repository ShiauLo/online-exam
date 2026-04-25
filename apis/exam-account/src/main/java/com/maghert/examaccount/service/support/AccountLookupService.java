package com.maghert.examaccount.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.maghert.examaccount.mapper.AccountMapper;
import com.maghert.examcommon.pojo.entity.SysUser;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class AccountLookupService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$");

    private final AccountMapper accountMapper;

    public AccountLookupService(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    public SysUser findByPhone(String phone) {
        return accountMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhoneNumber, phone));
    }

    public SysUser findByLoginAccount(String account) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        if (isPhone(account)) {
            return accountMapper.selectOne(queryWrapper.eq(SysUser::getPhoneNumber, account));
        }
        if (isEmail(account)) {
            return accountMapper.selectOne(queryWrapper.eq(SysUser::getEmail, account));
        }
        return accountMapper.selectOne(queryWrapper.eq(SysUser::getUsername, account));
    }

    private boolean isPhone(String account) {
        return account != null && PHONE_PATTERN.matcher(account).matches();
    }

    private boolean isEmail(String account) {
        return account != null && EMAIL_PATTERN.matcher(account).matches();
    }
}