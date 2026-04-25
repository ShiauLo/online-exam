package com.maghert.examcommon.apitest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maghert.examcommon.apitest.collector.ApiMetadataCollector;
import com.maghert.examcommon.apitest.collector.SpringControllerApiMetadataCollector;
import com.maghert.examcommon.apitest.execution.HttpApiLoadExecutor;
import com.maghert.examcommon.apitest.generation.ApiScenarioPlanner;
import com.maghert.examcommon.apitest.generation.ApiTestSuiteBuilder;
import com.maghert.examcommon.apitest.generation.ConstraintIntrospector;
import com.maghert.examcommon.apitest.generation.SampleValueResolverRegistry;
import com.maghert.examcommon.apitest.model.ApiTestSuite;
import com.maghert.examcommon.apitest.persistence.JsonApiTestSuiteRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.net.URI;
import java.nio.file.Files;
import java.time.OffsetDateTime;

public class ApiTestFacade {

    private final RequestMappingHandlerMapping handlerMapping;
    private final ObjectMapper objectMapper;
    private final ApiTestProperties properties;
    private final String applicationName;

    public ApiTestFacade(
            RequestMappingHandlerMapping handlerMapping,
            ObjectMapper objectMapper,
            ApiTestProperties properties,
            String applicationName) {
        this.handlerMapping = handlerMapping;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.applicationName = applicationName;
    }

    public ApiTestSuite generateAndPersist() throws Exception {
        String serviceName = serviceName();
        ApiMetadataCollector collector = new SpringControllerApiMetadataCollector(handlerMapping, properties.getControllerBasePackage());
        ApiTestSuiteBuilder builder = new ApiTestSuiteBuilder(
                new ConstraintIntrospector(),
                new ApiScenarioPlanner(new SampleValueResolverRegistry()));
        ApiTestSuite suite = builder.build(serviceName, collector.collect());
        JsonApiTestSuiteRepository repository = repository();
        repository.writeGeneratedSuite(serviceName, suite);
        ensureManualTemplate(serviceName);
        return suite;
    }

    public ApiTestSuite loadMergedSuite() throws Exception {
        return repository().loadMergedSuite(serviceName());
    }

    public HttpApiLoadExecutor.LoadSummary runIntegrationLoad(URI baseUri, int concurrency, int iterationsPerScenario) throws Exception {
        ApiTestSuite suite = loadMergedSuite();
        if (suite == null) {
            suite = generateAndPersist();
        }
        return new HttpApiLoadExecutor(objectMapper).execute(
                suite,
                baseUri,
                new HttpApiLoadExecutor.LoadProfile(concurrency, iterationsPerScenario));
    }

    public void ensureManualTemplate() throws Exception {
        ensureManualTemplate(serviceName());
    }

    private void ensureManualTemplate(String serviceName) throws Exception {
        JsonApiTestSuiteRepository repository = repository();
        var manualPath = repository.manualPath(serviceName);
        if (Files.exists(manualPath)) {
            return;
        }
        Files.createDirectories(manualPath.getParent());
        ApiTestSuite template = new ApiTestSuite();
        template.setServiceName(serviceName);
        template.setGeneratedAt(OffsetDateTime.now().toString());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(manualPath.toFile(), template);
    }

    private JsonApiTestSuiteRepository repository() {
        return new JsonApiTestSuiteRepository(objectMapper, properties.getOutputDir());
    }

    private String serviceName() {
        if (StringUtils.hasText(properties.getServiceName())) {
            return properties.getServiceName();
        }
        if (StringUtils.hasText(applicationName)) {
            return applicationName;
        }
        throw new IllegalStateException("No service name configured for API test generation.");
    }
}


