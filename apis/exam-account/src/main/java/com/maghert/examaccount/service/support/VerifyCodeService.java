package com.maghert.examaccount.service.support;

import com.maghert.examaccount.config.AliyunSmsProperties;
import com.maghert.examaccount.utils.VerifyCodeUtil;
import com.maghert.examcommon.exception.SendCodeException;
import com.maghert.examcommon.exception.VerifyCodeNotEqualException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static com.maghert.examaccount.constants.AccountConstants.REDIS_VERIFY_CODE_LOGIN;

@Service
public class VerifyCodeService {

    private final RedisTemplate<String, String> redisTemplate;
    private final VerifyCodeUtil verifyCodeUtil;
    private final AliyunSmsProperties smsProperties;

    public VerifyCodeService(
            RedisTemplate<String, String> redisTemplate,
            VerifyCodeUtil verifyCodeUtil,
            AliyunSmsProperties smsProperties) {
        this.redisTemplate = redisTemplate;
        this.verifyCodeUtil = verifyCodeUtil;
        this.smsProperties = smsProperties;
    }

    public String generateCode() {
        return verifyCodeUtil.randomCode(com.maghert.examaccount.constants.AccountConstants.VERIFY_CODE_LENGTH);
    }

    public void cacheLoginCode(String phoneNumber, String verifyCode) {
        redisTemplate.opsForValue().set(
                REDIS_VERIFY_CODE_LOGIN + phoneNumber,
                Objects.requireNonNull(verifyCode),
                com.maghert.examaccount.constants.AccountConstants.VERIFY_CODE_TTL,
                java.util.concurrent.TimeUnit.MINUTES);
    }

    public void validateLoginCode(String phoneNumber, String verifyCode) throws VerifyCodeNotEqualException {
        String cachedCode = redisTemplate.opsForValue().get(REDIS_VERIFY_CODE_LOGIN + phoneNumber);
        if (cachedCode == null || !cachedCode.equals(verifyCode)) {
            throw new VerifyCodeNotEqualException();
        }
    }

    public String renderSmsTemplate(String verifyCode) {
        return smsProperties.getTemplateParam()
                .replaceAll(com.maghert.examaccount.constants.AccountConstants.REPLACE_VERIFY_CODE, verifyCode)
                .replaceAll(com.maghert.examaccount.constants.AccountConstants.REPLACE_VERIFY_TTL,
                        com.maghert.examaccount.constants.AccountConstants.VERIFY_CODE_TTL.toString());
    }

    public SendCodeException sendFailure() {
        return new SendCodeException(500, "send verify code failed");
    }
}
