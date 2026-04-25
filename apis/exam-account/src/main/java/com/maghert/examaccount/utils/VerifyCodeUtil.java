package com.maghert.examaccount.utils;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class VerifyCodeUtil {


    /**
     * 生成指定长度的数字验证码（0-9）
     * @param length 验证码长度，必须≥1且≤8（避免过长）
     * @return 数字验证码字符串
     * @throws IllegalArgumentException 长度非法时抛出
     */
    public String randomCode(int length) {
        // 1. 严格参数校验，避免非法长度
        if (length < 1 || length > 8) {
            throw new IllegalArgumentException("验证码长度必须在1-8之间");
        }

        // 2. 使用ThreadLocalRandom（推荐），复用随机数实例，保证随机性
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(length); // 预指定容量，性能更好

        // 3. 循环生成随机数字
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10)); // 0-9的随机数
        }

        return sb.toString();
    }
}
