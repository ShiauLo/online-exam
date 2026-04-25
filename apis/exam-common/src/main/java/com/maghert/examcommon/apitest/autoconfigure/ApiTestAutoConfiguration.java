package com.maghert.examcommon.apitest.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maghert.examcommon.apitest.ApiTestFacade;
import com.maghert.examcommon.apitest.ApiTestProperties;
import com.maghert.examcommon.apitest.bootstrap.ApiTestSuiteBootstrap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@AutoConfiguration
@ConditionalOnClass(RequestMappingHandlerMapping.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@EnableConfigurationProperties(ApiTestProperties.class)
public class ApiTestAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(RequestMappingHandlerMapping.class)
    public ApiTestFacade apiTestFacade(
            RequestMappingHandlerMapping handlerMapping,
            ObjectMapper objectMapper,
            ApiTestProperties properties,
            @Value("${spring.application.name:}") String applicationName) {
        return new ApiTestFacade(handlerMapping, objectMapper, properties, applicationName);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ApiTestFacade.class)
    public ApiTestSuiteBootstrap apiTestSuiteBootstrap(
            ApiTestProperties properties,
            ApiTestFacade apiTestFacade) {
        return new ApiTestSuiteBootstrap(properties, apiTestFacade);
    }
}
