package com.maghert.examclass.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maghert.examclass.context.RequestContextResolver;
import com.maghert.examclass.handler.GlobalExceptionHandler;
import com.maghert.examclass.model.dto.ClassCreateRequest;
import com.maghert.examclass.service.ClassService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Objects;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClassControllerAdviceTests {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ClassService classService = Mockito.mock(ClassService.class);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
        ReflectionTestUtils.setField(exceptionHandler, "activeEnv", "dev");
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new ClassController(classService, new RequestContextResolver()))
                .setValidator(validator)
                .setControllerAdvice(exceptionHandler)
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldReturnStructured400ForValidationFailure() throws Exception {
        ClassCreateRequest request = new ClassCreateRequest();

        mockMvc.perform(post("/api/class/create")
                        .contentType(Objects.requireNonNull(APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("参数错误"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.timestamp").isNumber())
                .andExpect(jsonPath("$.errors").isArray());
    }
}
