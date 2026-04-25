package com.maghert.examcommon.apitest.bootstrap;

import com.maghert.examcommon.apitest.ApiTestFacade;
import com.maghert.examcommon.apitest.ApiTestProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

public class ApiTestSuiteBootstrap implements ApplicationRunner {

    private final ApiTestProperties properties;
    private final ApiTestFacade apiTestFacade;

    public ApiTestSuiteBootstrap(
            ApiTestProperties properties,
            ApiTestFacade apiTestFacade) {
        this.properties = properties;
        this.apiTestFacade = apiTestFacade;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!properties.isEnabled() || !properties.isGenerateOnStartup()) {
            return;
        }
        apiTestFacade.generateAndPersist();
    }
}


