package com.maghert.examquestion.controller;

import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examquestion.service.QuestionService;
import com.maghert.examquestion.vo.QuestionExportView;
import com.maghert.examquestion.vo.QuestionImportResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuestionControllerMultipartTests {

    private MockMvc mockMvc;
    private QuestionService questionService;

    @BeforeEach
    void setUp() {
        questionService = Mockito.mock(QuestionService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new QuestionController(questionService))
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldAcceptMultipartImport() throws Exception {
        Mockito.when(questionService.importQuestions(any(), anyLong()))
                .thenReturn(ApiResponse.ok(new QuestionImportResult(1, 0, java.util.List.of()))
                        .withRequestId("req-import"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "questions.csv",
                "text/csv",
                "content,type,answer,analysis,difficulty,options".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/question/import")
                        .file(file)
                        .param("categoryId", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("success"))
                .andExpect(jsonPath("$.data.importedCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(0))
                .andExpect(jsonPath("$.requestId").value("req-import"));
    }

    @Test
    void shouldReturnExportMetadata() throws Exception {
        Mockito.when(questionService.exportQuestions(any(), any()))
                .thenReturn(ApiResponse.ok(QuestionExportView.builder()
                                .fileKey("question-export-001")
                                .fileName("question-export-20260420.csv")
                                .recordCount(3)
                                .masked(false)
                                .build())
                        .withRequestId("req-export"));

        mockMvc.perform(get("/api/question/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("req-export"))
                .andExpect(jsonPath("$.data.fileKey").value("question-export-001"))
                .andExpect(jsonPath("$.data.recordCount").value(3))
                .andExpect(jsonPath("$.data.masked").value(false));
    }
}
