package com.maghert.examaccount.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static com.maghert.examaccount.constants.AccountConstants.REPLACE_VERIFY_CODE;

/**
 * 阿里云短信配置属性类（专门存放所有短信相关配置）
 * 绑定 YAML 中 "aliyun.sms" 前缀的配置
 */
@Data // Lombok 自动生成getter/setter/toString
@Component // 交给Spring管理
@ConfigurationProperties(prefix = "aliyun.sms") // 绑定配置前缀
public class AliyunSmsProperties {
    /** 短信服务端点（官方默认：dysmsapi.aliyuncs.com） */
    private String endpoint ;

    /** 短信签名（阿里云控制台已审核） */
    private String signName;

    /** 短信模板CODE（阿里云控制台已审核） */
    private String templateCode;

    /** 节点发送区域*/
    private String region;

    /** 短信模板参数*/
    private String templateParam = "{\"code\":\""+REPLACE_VERIFY_CODE+"\",\"min\":\"5\"}";

}