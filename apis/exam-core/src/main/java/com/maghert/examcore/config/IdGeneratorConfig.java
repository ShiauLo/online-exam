package com.maghert.examcore.config;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class IdGeneratorConfig {

    @Bean
    @Primary
    public Snowflake snowflake() {
        return IdUtil.getSnowflake(1, 1);
    }
}
