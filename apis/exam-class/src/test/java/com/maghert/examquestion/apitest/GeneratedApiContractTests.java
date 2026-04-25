package com.maghert.examquestion.apitest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maghert.examquestion.controller.QuestionController;
import com.maghert.examquestion.dto.QuestionAuditRequest;
import com.maghert.examquestion.dto.QuestionCategoryQueryRequest;
import com.maghert.examquestion.dto.QuestionCategoryUpsertRequest;
import com.maghert.examquestion.dto.QuestionCreateRequest;
import com.maghert.examquestion.dto.QuestionDeleteRequest;
import com.maghert.examquestion.dto.QuestionQueryRequest;
import com.maghert.examquestion.dto.QuestionToggleStatusRequest;
import com.maghert.examquestion.dto.QuestionUpdateRequest;
import com.maghert.examquestion.service.QuestionService;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examcommon.web.PageResult;
import com.maghert.examquestion.vo.QuestionCategoryView;
import com.maghert.examquestion.vo.QuestionExportView;
import com.maghert.examquestion.vo.QuestionImportResult;
import com.maghert.examquestion.vo.QuestionView;
import com.maghert.examcommon.apitest.collector.ReflectionControllerApiMetadataCollector;
import com.maghert.examcommon.apitest.generation.ApiScenarioPlanner;
import com.maghert.examcommon.apitest.generation.ApiTestSuiteBuilder;
import com.maghert.examcommon.apitest.generation.ConstraintIntrospector;
import com.maghert.examcommon.apitest.generation.SampleValueResolverRegistry;
import com.maghert.examcommon.apitest.model.ApiEndpointSpec;
import com.maghert.examcommon.apitest.model.ApiScenarioSpec;
import com.maghert.examcommon.apitest.model.ApiTestSuite;
import com.maghert.examcommon.apitest.persistence.JsonApiTestSuiteRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GeneratedApiContractTests {

    @Mock
    private QuestionService questionService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private ApiTestSuite suite;

    @BeforeAll
    void initSuite() throws Exception {
        objectMapper = new ObjectMapper();
        ApiTestSuiteBuilder builder = new ApiTestSuiteBuilder(
                new ConstraintIntrospector(),
                new ApiScenarioPlanner(new SampleValueResolverRegistry()));
        suite = builder.build(
                "exam-question",
                new ReflectionControllerApiMetadataCollector(
                        List.of(QuestionController.class),
                        "com.maghert.examquestion").collect());
        JsonApiTestSuiteRepository repository = new JsonApiTestSuiteRepository(objectMapper, "api-test-cases");
        repository.writeGeneratedSuite("exam-question", suite);
        suite = repository.loadMergedSuite("exam-question");
    }

    @BeforeEach
    void setUp() throws Exception {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new QuestionController(questionService))
                .setValidator(validator)
                .build();

        Mockito.reset(questionService);
        Mockito.when(questionService.create(any(QuestionCreateRequest.class))).thenReturn(ApiResponse.ok(sampleQuestion()));
        Mockito.when(questionService.update(any(QuestionUpdateRequest.class))).thenReturn(ApiResponse.ok(sampleQuestion()));
        Mockito.when(questionService.delete(any(QuestionDeleteRequest.class))).thenReturn(ApiResponse.ok("ok"));
        Mockito.when(questionService.toggleStatus(any(QuestionToggleStatusRequest.class))).thenReturn(ApiResponse.ok(sampleQuestion()));
        Mockito.when(questionService.createCategory(any(QuestionCategoryUpsertRequest.class)))
                .thenReturn(ApiResponse.ok(sampleCategory()));
        Mockito.when(questionService.updateCategory(any(QuestionCategoryUpsertRequest.class)))
                .thenReturn(ApiResponse.ok(sampleCategory()));
        Mockito.when(questionService.queryCategories(any(QuestionCategoryQueryRequest.class)))
                .thenReturn(ApiResponse.ok(new PageResult<>(new ArrayList<>(), 0, 1, 10)));
        Mockito.when(questionService.importQuestions(any(), anyLong()))
                .thenReturn(ApiResponse.ok(new QuestionImportResult(1, 0, List.of())));
        Mockito.when(questionService.exportQuestions(any(), any()))
                .thenReturn(ApiResponse.ok(QuestionExportView.builder()
                                .fileKey("question-export-001")
                                .fileName("question-export-20260421.csv")
                                .recordCount(1)
                                .masked(false)
                                .build()));
        Mockito.when(questionService.audit(any(QuestionAuditRequest.class))).thenReturn(ApiResponse.ok(sampleQuestion()));
        Mockito.when(questionService.query(any(QuestionQueryRequest.class)))
                .thenReturn(ApiResponse.ok(new PageResult<>(new ArrayList<>(), 0, 1, 10)));
    }

    @Test
    void shouldGenerateExecutableNegativeCasesForCreateWhenValidationIsBound() {
        ApiEndpointSpec createEndpoint = suite.getEndpoints().stream()
                .filter(endpoint -> "POST /api/question/create".equals(endpoint.getEndpointKey()))
                .findFirst()
                .orElseThrow();

        ApiScenarioSpec validationScenario = createEndpoint.getScenarios().stream()
                .filter(ApiScenarioSpec::isContractExecutable)
                .filter(scenario -> scenario.getExpectedStatus() == 400)
                .findFirst()
                .orElse(null);
        assertNotNull(validationScenario);
        Assertions.assertEquals(400, validationScenario.getExpectedStatus());
        Assertions.assertTrue(validationScenario.isContractExecutable());
    }

    @Test
    void shouldRejectQueryWhenPageNumMissing() throws Exception {
        QuestionQueryRequest queryRequest = new QuestionQueryRequest();
        queryRequest.setPageSize(10);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/question/query")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(queryRequest))))
                .andExpect(status().isBadRequest());
    }

    @TestFactory
    Stream<DynamicTest> shouldExecuteAllContractExecutableCases() {
        List<DynamicTest> tests = new ArrayList<>();
        for (ApiEndpointSpec endpoint : suite.getEndpoints()) {
            if (skipEndpoint(endpoint.getEndpointKey())) {
                continue;
            }
            for (ApiScenarioSpec scenario : endpoint.getScenarios()) {
                if (!scenario.isContractExecutable()) {
                    continue;
                }
                tests.add(DynamicTest.dynamicTest(
                        endpoint.getEndpointKey() + " :: " + scenario.getName(),
                        () -> execute(endpoint, scenario)));
            }
        }
        return tests.stream();
    }

    private void execute(ApiEndpointSpec endpoint, ApiScenarioSpec scenario) throws Exception {
        String path = Objects.requireNonNull(endpoint.getPath());
        MockHttpServletRequestBuilder builder = switch (endpoint.getHttpMethod()) {
            case "POST" -> MockMvcRequestBuilders.post(path);
            case "PUT" -> MockMvcRequestBuilders.put(path);
            case "DELETE" -> MockMvcRequestBuilders.delete(path);
            default -> MockMvcRequestBuilders.get(path);
        };
        if (endpoint.getRequestBodyClass() != null) {
            builder.contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                    .content(Objects.requireNonNull(objectMapper.writeValueAsString(scenario.getRequestBody())));
        }
        mockMvc.perform(builder)
                .andExpect(status().is(scenario.getExpectedStatus()));
    }

    private boolean skipEndpoint(String endpointKey) {
        return endpointKey.endsWith("/import") || endpointKey.endsWith("/export");
    }

    private QuestionView sampleQuestion() {
        return QuestionView.builder()
                .questionId(1L)
                .categoryId(10L)
                .creatorId(1001L)
                .content("sample question")
                .type("single")
                .options(List.of("A", "B"))
                .answer("A")
                .analysis("sample analysis")
                .difficulty(3)
                .auditStatus("approved")
                .isDisabled(false)
                .referenceCount(0)
                .build();
    }

    private QuestionCategoryView sampleCategory() {
        return QuestionCategoryView.builder()
                .categoryId(10L)
                .name("sample category")
                .parentId(null)
                .isPersonal(true)
                .ownerId(1001L)
                .build();
    }
}
