package com.maghert.examscore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.exception.DomainErrorCode;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examscore.context.RequestContextResolver;
import com.maghert.examscore.handler.GlobalExceptionHandler;
import com.maghert.examscore.model.dto.ScoreApplyRecheckRequest;
import com.maghert.examscore.model.dto.ScoreAnalyzeRequest;
import com.maghert.examscore.model.dto.ScoreHandleAppealRequest;
import com.maghert.examscore.model.dto.ScoreUpdateRequest;
import com.maghert.examscore.model.vo.ScoreAnalyzeView;
import com.maghert.examscore.model.vo.ScoreApplyRecheckView;
import com.maghert.examscore.model.vo.ScoreExportView;
import com.maghert.examscore.model.vo.ScoreHandleAppealView;
import com.maghert.examscore.model.vo.ScoreUpdateView;
import com.maghert.examscore.service.ExamScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExamScoreControllerAdviceTests {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private ExamScoreService examScoreService;

    @BeforeEach
    void setUp() {
        examScoreService = Mockito.mock(ExamScoreService.class);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
        ReflectionTestUtils.setField(exceptionHandler, "activeEnv", "dev");
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new ExamScoreController(examScoreService, new RequestContextResolver()))
                .setValidator(validator)
                .setControllerAdvice(exceptionHandler)
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldReturnStructured400WhenAnalyzeRequestInvalid() throws Exception {
        ScoreAnalyzeRequest request = new ScoreAnalyzeRequest();

        mockMvc.perform(post("/api/score/analyze")
                        .contentType(Objects.requireNonNull(APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("参数错误"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void shouldReturnAnalyzePayload() throws Exception {
        ScoreAnalyzeView response = ScoreAnalyzeView.builder()
                .examId(8801L)
                .examName("Java 期中")
                .passScore(60)
                .totalScore(100)
                .totalParticipants(2)
                .finishedParticipants(2)
                .generatedAt(LocalDateTime.of(2026, 5, 1, 11, 0))
                .overview(ScoreAnalyzeView.Overview.builder()
                        .averageScore(new BigDecimal("80.00"))
                        .highestScore(90)
                        .lowestScore(70)
                        .passCount(2)
                        .passRate(new BigDecimal("100.00"))
                        .build())
                .statusDistribution(List.of(
                        ScoreAnalyzeView.StatusDistributionItem.builder().status("PENDING").count(0).build(),
                        ScoreAnalyzeView.StatusDistributionItem.builder().status("SCORING").count(0).build(),
                        ScoreAnalyzeView.StatusDistributionItem.builder().status("SCORED").count(1).build(),
                        ScoreAnalyzeView.StatusDistributionItem.builder().status("PUBLISHED").count(1).build()))
                .scoreRangeDistribution(List.of(
                        ScoreAnalyzeView.ScoreRangeDistributionItem.builder().rangeLabel("80-89").count(1).build()))
                .classDistribution(List.of(
                        ScoreAnalyzeView.ClassDistributionItem.builder()
                                .classId(501L)
                                .className("Java 1班")
                                .studentCount(2)
                                .finishedCount(2)
                                .averageScore(new BigDecimal("80.00"))
                                .passRate(new BigDecimal("100.00"))
                                .build()))
                .build();
        Mockito.when(examScoreService.analyze(Mockito.any(), Mockito.any()))
                .thenReturn(ApiResponse.ok(response).withRequestId("req-analyze"));

        ScoreAnalyzeRequest request = new ScoreAnalyzeRequest();
        request.setExamId(8801L);

        mockMvc.perform(post("/api/score/analyze")
                        .header("X-User-Id", "3001")
                        .header("X-Role-Id", "3")
                        .header("X-Request-Id", "req-analyze")
                        .contentType(Objects.requireNonNull(APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.requestId").value("req-analyze"))
                .andExpect(jsonPath("$.data.examId").value(8801))
                .andExpect(jsonPath("$.data.overview.averageScore").value(80.0))
                .andExpect(jsonPath("$.data.statusDistribution[2].status").value("SCORED"))
                .andExpect(jsonPath("$.data.classDistribution[0].classId").value(501));
    }

    @Test
    void shouldReturnForbiddenWhenAnalyzeServiceRejectsRequest() throws Exception {
        Mockito.when(examScoreService.analyze(Mockito.any(), Mockito.any()))
                .thenThrow(new BusinessException(DomainErrorCode.SCORE_ANALYZE_FORBIDDEN));

        ScoreAnalyzeRequest request = new ScoreAnalyzeRequest();
        request.setExamId(8801L);

        mockMvc.perform(post("/api/score/analyze")
                        .header("X-User-Id", "4001")
                        .header("X-Role-Id", "4")
                        .header("X-Request-Id", "req-forbidden")
                        .contentType(Objects.requireNonNull(APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("无权限进行成绩分析"));
    }

    @Test
    void shouldReturnStructured400WhenApplyRecheckRequestInvalid() throws Exception {
        ScoreApplyRecheckRequest request = new ScoreApplyRecheckRequest();

        mockMvc.perform(post("/api/score/apply-recheck")
                        .contentType(Objects.requireNonNull(APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void shouldReturnApplyRecheckPayload() throws Exception {
        ScoreApplyRecheckView response = ScoreApplyRecheckView.builder()
                .appealId(9301L)
                .scoreId(9101L)
                .questionId(7001L)
                .status("PENDING")
                .recheckStatus("pending")
                .createdAt(LocalDateTime.of(2026, 5, 2, 9, 30))
                .build();
        Mockito.when(examScoreService.applyRecheck(Mockito.any(), Mockito.any()))
                .thenReturn(ApiResponse.ok(response).withRequestId("req-recheck"));

        ScoreApplyRecheckRequest request = new ScoreApplyRecheckRequest();
        request.setExamId(8801L);
        request.setStudentId(4001L);
        request.setQuestionId(7001L);
        request.setReason("申请复核");

        mockMvc.perform(post("/api/score/apply-recheck")
                        .header("X-User-Id", "4001")
                        .header("X-Role-Id", "4")
                        .header("X-Request-Id", "req-recheck")
                        .contentType(Objects.requireNonNull(APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("req-recheck"))
                .andExpect(jsonPath("$.data.appealId").value(9301))
                .andExpect(jsonPath("$.data.recheckStatus").value("pending"));
    }

    @Test
    void shouldReturnExportPayload() throws Exception {
        ScoreExportView response = ScoreExportView.builder()
                .fileKey("score-export-001")
                .fileName("score-export-exam-8801.csv")
                .recordCount(12)
                .includeAnalysis(true)
                .generatedAt(LocalDateTime.of(2026, 5, 2, 10, 0))
                .build();
        Mockito.when(examScoreService.export(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(ApiResponse.ok(response).withRequestId("req-export"));

        mockMvc.perform(get("/api/score/export")
                        .header("X-User-Id", "2001")
                        .header("X-Role-Id", "2")
                        .header("X-Request-Id", "req-export")
                        .param("examId", "8801")
                        .param("includeAnalysis", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileKey").value("score-export-001"))
                .andExpect(jsonPath("$.data.recordCount").value(12))
                .andExpect(jsonPath("$.data.includeAnalysis").value(true));
    }

    @Test
    void shouldReturnHandleAppealAndUpdatePayload() throws Exception {
        ScoreHandleAppealView appealResponse = ScoreHandleAppealView.builder()
                .appealId(9301L)
                .scoreId(9101L)
                .questionId(7001L)
                .status("APPROVED")
                .recheckStatus("processed")
                .handleResult("APPROVED")
                .scoreStatus("SCORING")
                .handledAt(LocalDateTime.of(2026, 5, 2, 11, 0))
                .build();
        Mockito.when(examScoreService.handleAppeal(Mockito.any(), Mockito.any()))
                .thenReturn(ApiResponse.ok(appealResponse).withRequestId("req-appeal"));

        ScoreHandleAppealRequest appealRequest = new ScoreHandleAppealRequest();
        appealRequest.setAppealId(9301L);
        appealRequest.setHandleResult("approved");
        appealRequest.setReason("重新批阅");

        mockMvc.perform(put("/api/score/handle-appeal")
                        .header("X-User-Id", "3001")
                        .header("X-Role-Id", "3")
                        .header("X-Request-Id", "req-appeal")
                        .contentType(Objects.requireNonNull(APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(appealRequest))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.scoreStatus").value("SCORING"));

        ScoreUpdateView updateResponse = ScoreUpdateView.builder()
                .scoreId(9101L)
                .previousTotalScore(86)
                .totalScore(95)
                .objectiveScore(60)
                .subjectiveScore(35)
                .approverId(1002L)
                .updatedAt(LocalDateTime.of(2026, 5, 2, 11, 10))
                .build();
        Mockito.when(examScoreService.update(Mockito.any(), Mockito.any()))
                .thenReturn(ApiResponse.ok(updateResponse).withRequestId("req-update"));

        ScoreUpdateRequest updateRequest = new ScoreUpdateRequest();
        updateRequest.setScoreId(9101L);
        updateRequest.setNewScore(95);
        updateRequest.setReason("调分");
        updateRequest.setApproverId(1002L);

        mockMvc.perform(put("/api/score/update")
                        .header("X-User-Id", "1001")
                        .header("X-Role-Id", "1")
                        .header("X-Request-Id", "req-update")
                        .contentType(Objects.requireNonNull(APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(updateRequest))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.previousTotalScore").value(86))
                .andExpect(jsonPath("$.data.totalScore").value(95))
                .andExpect(jsonPath("$.data.approverId").value(1002));
    }

    @Test
    void shouldReturnForbiddenWhenUpdateServiceRejectsRequest() throws Exception {
        Mockito.when(examScoreService.update(Mockito.any(), Mockito.any()))
                .thenThrow(new BusinessException(DomainErrorCode.SCORE_UPDATE_FORBIDDEN));

        ScoreUpdateRequest request = new ScoreUpdateRequest();
        request.setScoreId(9101L);
        request.setNewScore(95);
        request.setReason("无权限调分");
        request.setApproverId(1002L);

        mockMvc.perform(put("/api/score/update")
                        .header("X-User-Id", "2001")
                        .header("X-Role-Id", "2")
                        .header("X-Request-Id", "req-update-forbidden")
                        .contentType(Objects.requireNonNull(APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("无权限修改成绩"));
    }
}
