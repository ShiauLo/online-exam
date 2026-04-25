package com.maghert.examaccount.apitest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maghert.examaccount.controller.AccountController;
import com.maghert.examaccount.pojo.dto.AccountAuditDTO;
import com.maghert.examaccount.service.AccountService;
import com.maghert.examaccount.service.LoginService;
import com.maghert.examaccount.service.TokenService;
import com.maghert.examcommon.apitest.collector.ReflectionControllerApiMetadataCollector;
import com.maghert.examcommon.apitest.generation.ApiScenarioPlanner;
import com.maghert.examcommon.apitest.generation.ApiTestSuiteBuilder;
import com.maghert.examcommon.apitest.generation.ConstraintIntrospector;
import com.maghert.examcommon.apitest.generation.SampleValueResolverRegistry;
import com.maghert.examcommon.apitest.model.ApiEndpointSpec;
import com.maghert.examcommon.apitest.model.ApiScenarioSpec;
import com.maghert.examcommon.apitest.model.ApiTestSuite;
import com.maghert.examcommon.apitest.persistence.JsonApiTestSuiteRepository;
import com.maghert.examcommon.pojo.dto.AccountCreateDTO;
import com.maghert.examcommon.pojo.dto.AccountDeleteDTO;
import com.maghert.examcommon.pojo.dto.AccountFreezeDTO;
import com.maghert.examcommon.pojo.dto.AccountLogOutDTO;
import com.maghert.examcommon.pojo.dto.AccountPasswordResetDTO;
import com.maghert.examcommon.pojo.dto.AccountQueryDTO;
import com.maghert.examcommon.pojo.dto.AccountUpdateDTO;
import com.maghert.examcommon.pojo.dto.LoginDTO;
import com.maghert.examcommon.pojo.dto.RefreshTokenDTO;
import com.maghert.examcommon.pojo.dto.SendVerifyCodeDTO;
import com.maghert.examcommon.pojo.vo.LoginVO;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examcommon.web.PageResult;
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
    private AccountService accountService;

    @Mock
    private LoginService loginService;

    @Mock
    private TokenService tokenService;

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
                "exam-account",
                new ReflectionControllerApiMetadataCollector(
                        List.of(AccountController.class),
                        "com.maghert.examaccount").collect());
        JsonApiTestSuiteRepository repository = new JsonApiTestSuiteRepository(objectMapper, "api-test-cases");
        repository.writeGeneratedSuite("exam-account", suite);
        suite = repository.loadMergedSuite("exam-account");
    }

    @BeforeEach
    void setUp() throws Exception {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AccountController(accountService, loginService, tokenService))
                .setValidator(validator)
                .build();

        Mockito.reset(accountService, loginService, tokenService);
        Mockito.when(accountService.create(any(AccountCreateDTO.class), anyLong())).thenReturn(ApiResponse.ok("ok"));
        Mockito.when(accountService.sendVerifyCode(any(SendVerifyCodeDTO.class))).thenReturn(ApiResponse.ok("ok"));
        Mockito.when(accountService.query(any(AccountQueryDTO.class)))
                .thenReturn(ApiResponse.ok(new PageResult<>(new ArrayList<>(), 0, 1, 10)));
        Mockito.when(accountService.audit(any(AccountAuditDTO.class))).thenReturn(ApiResponse.ok("ok"));
        Mockito.when(accountService.freeze(any(AccountFreezeDTO.class))).thenReturn(ApiResponse.ok("ok"));
        Mockito.when(accountService.update(any(AccountUpdateDTO.class))).thenReturn(ApiResponse.ok("ok"));
        Mockito.when(accountService.delete(any(AccountDeleteDTO.class))).thenReturn(ApiResponse.ok("ok"));
        Mockito.when(accountService.resetPassword(any(AccountPasswordResetDTO.class))).thenReturn(ApiResponse.ok("ok"));
        Mockito.when(accountService.logout(any(AccountLogOutDTO.class))).thenReturn(ApiResponse.ok("ok"));
        Mockito.when(tokenService.refreshToken(any(String.class))).thenReturn(ApiResponse.ok("access-token"));
        Mockito.when(loginService.login(any(LoginDTO.class))).thenReturn(ApiResponse.ok(LoginVO.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build()));
    }

    @Test
    void shouldGenerateExecutableNegativeCasesForCreateWhenValidationIsBound() {
        ApiEndpointSpec createEndpoint = suite.getEndpoints().stream()
                .filter(endpoint -> "POST /api/account/create".equals(endpoint.getEndpointKey()))
                .findFirst()
                .orElseThrow();

        ApiScenarioSpec invalidFormat = createEndpoint.getScenarios().stream()
                .filter(scenario -> "phoneNumber_invalid_format".equals(scenario.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(invalidFormat);
        Assertions.assertEquals(400, invalidFormat.getExpectedStatus());
        Assertions.assertTrue(invalidFormat.isContractExecutable());
    }

    @Test
    void shouldRejectPasswordLoginWithoutAccountAndPassword() throws Exception {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setLoginType("password_login");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/account/login")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(loginDTO))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectOneKeyLoginWithoutPhoneAndVerifyCode() throws Exception {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setLoginType("one_key_login");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/account/login")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(loginDTO))))
                .andExpect(status().isBadRequest());
    }

    @TestFactory
    Stream<DynamicTest> shouldExecuteAllContractExecutableCases() {
        List<DynamicTest> tests = new ArrayList<>();
        for (ApiEndpointSpec endpoint : suite.getEndpoints()) {
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
}
