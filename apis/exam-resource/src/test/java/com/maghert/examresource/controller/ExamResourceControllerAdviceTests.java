package com.maghert.examresource.controller;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.exception.DomainErrorCode;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examresource.context.RequestContextResolver;
import com.maghert.examresource.handler.GlobalExceptionHandler;
import com.maghert.examresource.model.vo.ResourceFileDownloadView;
import com.maghert.examresource.model.vo.ResourcePaperTemplateView;
import com.maghert.examresource.model.vo.ResourceQuestionImageView;
import com.maghert.examresource.service.ExamResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExamResourceControllerAdviceTests {

    private MockMvc mockMvc;
    private ExamResourceService examResourceService;

    @BeforeEach
    void setUp() {
        examResourceService = Mockito.mock(ExamResourceService.class);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
        ReflectionTestUtils.setField(exceptionHandler, "activeEnv", "dev");
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new ExamResourceController(examResourceService, new RequestContextResolver()))
                .setValidator(validator)
                .setControllerAdvice(exceptionHandler)
                .build();
    }

    @Test
    void shouldReturnStructured400WhenDownloadServiceRejectsInvalidFileKey() throws Exception {
        Mockito.when(examResourceService.downloadFile(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenThrow(new BusinessException(DomainErrorCode.RESOURCE_KEY_INVALID));

        mockMvc.perform(get("/api/resource/file/download")
                        .header("X-User-Id", "2001")
                        .header("X-Role-Id", "2")
                        .header("X-Request-Id", "req-invalid")
                        .param("fileKey", "../bad-key"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("资源标识不合法"));
    }

    @Test
    void shouldReturnDownloadPayload() throws Exception {
        ResourceFileDownloadView response = ResourceFileDownloadView.builder()
                .fileKey("score-export-001")
                .fileName("score-export-001.csv")
                .contentType("text/csv;charset=UTF-8")
                .downloadPath("/api/resource/file/download?fileKey=score-export-001")
                .size(128L)
                .build();
        Mockito.when(examResourceService.downloadFile(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(ApiResponse.ok(response).withRequestId("req-download"));

        mockMvc.perform(get("/api/resource/file/download")
                        .header("X-User-Id", "3001")
                        .header("X-Role-Id", "3")
                        .header("X-Request-Id", "req-download")
                        .param("fileKey", "score-export-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("req-download"))
                .andExpect(jsonPath("$.data.fileKey").value("score-export-001"))
                .andExpect(jsonPath("$.data.fileName").value("score-export-001.csv"));
    }

    @Test
    void shouldReturnQuestionImageAndTemplatePayload() throws Exception {
        ResourceQuestionImageView questionImage = ResourceQuestionImageView.builder()
                .imgKey("question-img-001")
                .fileName("question-img-001.svg")
                .contentType("image/svg+xml")
                .accessPath("/api/resource/question/img?imgKey=question-img-001")
                .size(256L)
                .build();
        Mockito.when(examResourceService.questionImage(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(ApiResponse.ok(questionImage).withRequestId("req-img"));

        mockMvc.perform(get("/api/resource/question/img")
                        .header("X-User-Id", "3001")
                        .header("X-Role-Id", "3")
                        .header("X-Request-Id", "req-img")
                        .param("imgKey", "question-img-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imgKey").value("question-img-001"));

        ResourcePaperTemplateView template = ResourcePaperTemplateView.builder()
                .paperType("single")
                .fileName("paper-template-single.csv")
                .contentType("text/csv;charset=UTF-8")
                .downloadPath("/api/resource/paper/template?paperType=single")
                .size(512L)
                .build();
        Mockito.when(examResourceService.paperTemplate(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(ApiResponse.ok(template).withRequestId("req-template"));

        mockMvc.perform(get("/api/resource/paper/template")
                        .header("X-User-Id", "3001")
                        .header("X-Role-Id", "3")
                        .header("X-Request-Id", "req-template")
                        .param("paperType", "single"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paperType").value("single"));
    }

    @Test
    void shouldReturnForbiddenWhenDownloadServiceRejectsRequest() throws Exception {
        Mockito.when(examResourceService.downloadFile(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenThrow(new BusinessException(DomainErrorCode.RESOURCE_ACCESS_FORBIDDEN));

        mockMvc.perform(get("/api/resource/file/download")
                        .header("X-User-Id", "4001")
                        .header("X-Role-Id", "4")
                        .header("X-Request-Id", "req-forbidden")
                        .param("fileKey", "system-log-export-001"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("无权限访问资源"));
    }
}
