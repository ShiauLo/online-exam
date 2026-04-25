package com.maghert.examaccount.config;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdGeneratorConfig {


    /**
     * 创建一个雪花算法ID生成器的Bean
     *
     * @return Snowflake 实例
     */
    @Bean
    public Snowflake snowflakeIdGenerator() {
        // workerId 和 dataCenterId 需要保证在你的分布式集群中是唯一的
        // 它们的取值范围都是 0-31
        long workerId = 1;
        long dataCenterId = 1;

        // 在实际生产中，你可能需要通过配置文件、环境变量或其他方式动态获取 workerId
        // 例如: workerId = Long.parseLong(env.getProperty("snowflake.worker-id"));

        return IdUtil.getSnowflake(workerId, dataCenterId);
    }
}
