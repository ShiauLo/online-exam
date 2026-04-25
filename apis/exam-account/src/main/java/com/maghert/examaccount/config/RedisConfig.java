package com.maghert.examaccount.config;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 1. 配置 Jackson 的 ObjectMapper（与原逻辑保持一致）
        ObjectMapper objectMapper = new ObjectMapper();
        // 允许访问所有字段（包括private）
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 启用类型信息存储（反序列化时能识别对象类型）
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL // 非final类支持类型嵌入
        );

        // 2. 使用 GenericJackson2JsonRedisSerializer（替代原 Jackson2JsonRedisSerializer）
        StringRedisSerializer jsonSerializer = new StringRedisSerializer();

        // 3. 配置 Key/Value 的序列化方式
        template.setKeySerializer(new StringRedisSerializer()); // Key用字符串序列化
        template.setValueSerializer(jsonSerializer); // Value用GenericJackson序列化

        template.setHashKeySerializer(new StringRedisSerializer()); // HashKey用字符串序列化
        template.setHashValueSerializer(jsonSerializer); // HashValue用GenericJackson序列化

        template.afterPropertiesSet();
        return template;
    }
}