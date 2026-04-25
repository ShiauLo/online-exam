package com.maghert.examclass.config;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class IdGeneratorConfig {

    @Bean
    @Primary
    public Snowflake snowflakeIdGenerator() {
        return IdUtil.getSnowflake(1, 2);
    }
}
