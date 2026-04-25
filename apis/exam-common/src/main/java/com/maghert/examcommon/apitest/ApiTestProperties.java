package com.maghert.examcommon.apitest;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "exam.api-test")
public class ApiTestProperties {

    private boolean enabled;

    private boolean generateOnStartup = true;

    private String serviceName;

    private String outputDir = "api-test-cases";

    private String controllerBasePackage;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isGenerateOnStartup() {
        return generateOnStartup;
    }

    public void setGenerateOnStartup(boolean generateOnStartup) {
        this.generateOnStartup = generateOnStartup;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public String getControllerBasePackage() {
        return controllerBasePackage;
    }

    public void setControllerBasePackage(String controllerBasePackage) {
        this.controllerBasePackage = controllerBasePackage;
    }
}


