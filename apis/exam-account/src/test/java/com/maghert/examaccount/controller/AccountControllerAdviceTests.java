package com.maghert.examaccount.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maghert.examaccount.handler.GlobalExceptionHandler;
import com.maghert.examaccount.service.TokenService;
import com.maghert.examaccount.service.AccountService;
import com.maghert.examaccount.service.LoginService;
import com.maghert.examcommon.exception.UpdateRedisException;
import com.maghert.examcommon.exception.UserNotExistsException;
import com.maghert.examcommon.constants.AuthConstants;
import com.maghert.examcommon.pojo.dto.AccountDeleteDTO;
import com.maghert.examcommon.pojo.dto.AccountLogOutDTO;
import com.maghert.examcommon.pojo.dto.AccountPasswordResetDTO;
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

class AccountControllerAdviceTests {

    private static final String PARAM_ERROR = "\u53c2\u6570\u9519\u8bef";

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = Mockito.mock(AccountService.class);
        LoginService loginService = Mockito.mock(LoginService.class);
        TokenService tokenService = Mockito.mock(TokenService.class);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
        ReflectionTestUtils.setField(exceptionHandler, "activeEnv", "dev");
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new AccountController(accountService, loginService, tokenService))
                .setValidator(validator)
                .setControllerAdvice(exceptionHandler)
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldReturnStructured400ForValidationFailure() throws Exception {
        AccountDeleteDTO dto = new AccountDeleteDTO();

        mockMvc.perform(delete("/api/account/delete")
                        .header(AuthConstants.INTERNAL_REQUEST_ID_HEADER, "x-request-1")
                        .header(AuthConstants.REQUEST_ID_HEADER, "request-1")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(dto))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value(PARAM_ERROR))
                .andExpect(jsonPath("$.requestId").value("x-request-1"))
                .andExpect(jsonPath("$.timestamp").isNumber())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]").isNotEmpty());
    }

    @Test
    void shouldMapUserNotExistsTo404() throws Exception {
        Mockito.when(accountService.resetPassword(any(AccountPasswordResetDTO.class)))
                .thenThrow(new UserNotExistsException());
        AccountPasswordResetDTO dto = new AccountPasswordResetDTO(1L, "NewPassword#1", "123456");

        mockMvc.perform(put("/api/account/reset-password")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(dto))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value(new UserNotExistsException().getMessage()))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.timestamp").isNumber());
    }

    @Test
    void shouldMapRedisFailureTo500() throws Exception {
        Mockito.when(accountService.logout(any(AccountLogOutDTO.class))).thenThrow(new UpdateRedisException());
        AccountLogOutDTO dto = new AccountLogOutDTO(1L, "refresh-token");

        mockMvc.perform(post("/api/account/logout")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(dto))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value(new UpdateRedisException().getMessage()))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.timestamp").isNumber());
    }
}
