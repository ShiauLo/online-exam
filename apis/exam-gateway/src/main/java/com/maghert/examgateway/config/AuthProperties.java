package com.maghert.examgateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "exam.auth")
@Data
public class AuthProperties {

    private List<String> ignorePaths;

}
