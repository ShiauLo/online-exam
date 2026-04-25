package com.maghert.examissuecore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.exception.DomainErrorCode;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examcommon.web.PageResult;
import com.maghert.examissuecore.context.RequestContextResolver;
import com.maghert.examissuecore.handler.GlobalExceptionHandler;
import com.maghert.examissuecore.model.dto.IssueCreateRequest;
import com.maghert.examissuecore.model.dto.IssueHandleRequest;
import com.maghert.examissuecore.model.dto.IssueQueryRequest;
import com.maghert.examissuecore.model.dto.IssueTrackRequest;
import com.maghert.examissuecore.model.vo.IssueProcessLogView;
import com.maghert.examissuecore.model.vo.IssueTrackView;
import com.maghert.examissuecore.model.vo.IssueView;
import com.maghert.examissuecore.service.ExamIssueCoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExamIssueCoreControllerAdviceTests {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private ExamIssueCoreService examIssueCoreService;

    @BeforeEach
    void setUp() {
        examIssueCoreService = Mockito.mock(ExamIssueCoreService.class);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
        ReflectionTestUtils.setField(exceptionHandler, "activeEnv", "dev");
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new ExamIssueCoreController(examIssueCoreService, new RequestContextResolver()))
                .setValidator(validator)
                .setControllerAdvice(exceptionHandler)
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldReturnStructured400WhenCreateRequestInvalid() throws Exception {
        IssueCreateRequest request = new IssueCreateRequest();

        mockMvc.perform(post("/api/issue/core/create")
                        .contentType(Objects.requireNonNull(APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void shouldReturnQueryPayload() throws Exception {
        IssueView issueView = IssueView.builder()
                .issueId(9101L)
                .type("EXAM")
                .title("考试问题")
                .description("试卷无法加载")
                .status("PROCESSING")
                .reporterId(4001L)
                .reporterName("张三")
                .currentHandlerId(3001L)
                .currentHandlerName("李老师")
                .examId(8801L)
                .examName("Java 期中")
                .classId(501L)
                .className("Java 1班")
                .latestResult("处理中")
                .latestSolution("教师排查")
                .imgUrls(List.of("img-1"))
                .createTime(LocalDateTime.of(2026, 5, 3, 10, 0))
                .updateTime(LocalDateTime.of(2026, 5, 3, 10, 5))
                .build();
        Mockito.when(examIssueCoreService.query(Mockito.any(), Mockito.any()))
                .thenReturn(ApiResponse.ok(new PageResult<>(List.of(issueView), 1, 1, 10)).withRequestId("req-query"));

        IssueQueryRequest request = new IssueQueryRequest();
        request.setPageNum(1L);
        request.setPageSize(10L);

        mockMvc.perform(post("/api/issue/core/query")
                        .header("X-User-Id", "5001")
                        .header("X-Role-Id", "5")
                        .header("X-Request-Id", "req-query")
                        .contentType(Objects.requireNonNull(APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("req-query"))
                .andExpect(jsonPath("$.data.records[0].issueId").value(9101))
                .andExpect(jsonPath("$.data.records[0].type").value("EXAM"))
                .andExpect(jsonPath("$.data.records[0].currentHandlerName").value("李老师"));
    }

    @Test
    void shouldReturnForbiddenWhenHandleRejected() throws Exception {
        Mockito.when(examIssueCoreService.handle(Mockito.any(), Mockito.any()))
                .thenThrow(new BusinessException(DomainErrorCode.ISSUE_HANDLE_FORBIDDEN));

        IssueHandleRequest request = new IssueHandleRequest();
        request.setIssueId(9101L);
        request.setResult("处理中");
        request.setSolution("学生越权");

        mockMvc.perform(put("/api/issue/core/handle")
                        .header("X-User-Id", "4001")
                        .header("X-Role-Id", "4")
                        .header("X-Request-Id", "req-handle")
                        .contentType(Objects.requireNonNull(APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("无权限处理该问题"));
    }

    @Test
    void shouldReturnTrackPayload() throws Exception {
        IssueTrackView trackView = IssueTrackView.builder()
                .issueId(9101L)
                .type("EXAM")
                .title("考试问题")
                .status("CLOSED")
                .reporterId(4001L)
                .reporterName("张三")
                .currentHandlerId(3003L)
                .currentHandlerName("组长老师")
                .examId(8801L)
                .examName("Java 期中")
                .classId(501L)
                .className("Java 1班")
                .latestResult("继续处理")
                .latestSolution("问题已解决")
                .imgUrls(List.of("img-1"))
                .createTime(LocalDateTime.of(2026, 5, 3, 10, 0))
                .updateTime(LocalDateTime.of(2026, 5, 3, 11, 0))
                .logs(List.of(
                        IssueProcessLogView.builder()
                                .logId(1L)
                                .action("CREATED")
                                .operatorId(4001L)
                                .operatorName("张三")
                                .content("创建问题")
                                .occurredAt(LocalDateTime.of(2026, 5, 3, 10, 0))
                                .build(),
                        IssueProcessLogView.builder()
                                .logId(2L)
                                .action("CLOSED")
                                .operatorId(3003L)
                                .operatorName("组长老师")
                                .content("已确认")
                                .occurredAt(LocalDateTime.of(2026, 5, 3, 11, 0))
                                .build()))
                .build();
        Mockito.when(examIssueCoreService.track(Mockito.any(), Mockito.any()))
                .thenReturn(ApiResponse.ok(trackView).withRequestId("req-track"));

        IssueTrackRequest request = new IssueTrackRequest();
        request.setIssueId(9101L);

        mockMvc.perform(post("/api/issue/core/track")
                        .header("X-User-Id", "4001")
                        .header("X-Role-Id", "4")
                        .header("X-Request-Id", "req-track")
                        .contentType(Objects.requireNonNull(APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.issueId").value(9101))
                .andExpect(jsonPath("$.data.logs[0].action").value("CREATED"))
                .andExpect(jsonPath("$.data.logs[1].operatorName").value("组长老师"));
    }
}
