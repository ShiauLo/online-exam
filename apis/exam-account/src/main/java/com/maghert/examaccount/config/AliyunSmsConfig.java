package com.maghert.examaccount.config;

//import com.aliyun.credentials.Client; // 别名避免冲突
import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云短信客户端配置类（对齐官方SDK）
 * 核心：托管官方Client为Spring Bean，复用阿里云统一凭据链
 */
@Slf4j
@Configuration
@RequiredArgsConstructor // 构造器注入配置属性类
public class AliyunSmsConfig {

    //从properties中注入endpoint
    private final AliyunSmsProperties aliyunSmsProperties;

    /**
     * 创建官方短信Client并注册为Spring单例Bean
     * 凭据优先级：环境变量 > ~/.alibabacloud/credentials文件 > ECS实例RAM角色
     */
    @Bean
    public Client aliyunSmsClient() throws Exception {
        // 1. 初始化阿里云统一凭据客户端（官方推荐，自动走凭据链，无硬编码AK）
        com.aliyun.credentials.Client credentialClient = new com.aliyun.credentials.Client();

        // 2. 构建SDK配置（仅指定endpoint，凭据由credentialClient自动管理）
        Config config = new Config()
                .setCredential(credentialClient); // 绑定凭据客户端
        config.endpoint = aliyunSmsProperties.getEndpoint(); // 设置短信服务端点

        // 3. 创建官方短信Client并返回
        Client smsClient = new Client(config);
        return smsClient;
    }
}