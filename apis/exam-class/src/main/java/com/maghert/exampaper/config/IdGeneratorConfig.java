package com.maghert.exampaper.config;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdGeneratorConfig {

    @Bean
    public Snowflake snowflakeIdGenerator() {
        return IdUtil.getSnowflake(1, 4);
    }
}
