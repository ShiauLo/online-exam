package com.maghert.examquestion.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examquestion.dto.QuestionAuditRequest;
import com.maghert.examquestion.dto.QuestionDeleteRequest;
import com.maghert.examquestion.dto.QuestionQueryRequest;
import com.maghert.examquestion.handler.GlobalExceptionHandler;
import com.maghert.examquestion.service.QuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuestionControllerAdviceTests {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private QuestionService questionService;

    @BeforeEach
    void setUp() {
        questionService = Mockito.mock(QuestionService.class);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
        ReflectionTestUtils.setField(exceptionHandler, "activeEnv", "dev");
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new QuestionController(questionService))
                .setControllerAdvice(exceptionHandler)
                .setValidator(validator)
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldReturnStructured400ForValidationFailure() throws Exception {
        mockMvc.perform(delete("/api/question/delete")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(new QuestionDeleteRequest()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("parameter invalid"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.timestamp").isNumber())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void shouldMapBusinessExceptionTo403() throws Exception {
        Mockito.when(questionService.audit(any(QuestionAuditRequest.class)))
                .thenThrow(new BusinessException(403, "admin role required"));

        QuestionAuditRequest request = new QuestionAuditRequest();
        request.setQuestionId(1L);
        request.setAuditResult("approved");

        mockMvc.perform(put("/api/question/audit")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("admin role required"))
                .andExpect(jsonPath("$.requestId").isString());
    }

    @Test
    void shouldMapRuntimeExceptionTo500() throws Exception {
        Mockito.when(questionService.query(any(QuestionQueryRequest.class)))
                .thenThrow(new RuntimeException("boom"));

        QuestionQueryRequest request = new QuestionQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);

        mockMvc.perform(post("/api/question/query")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("boom"))
                .andExpect(jsonPath("$.requestId").isString());
    }
}
