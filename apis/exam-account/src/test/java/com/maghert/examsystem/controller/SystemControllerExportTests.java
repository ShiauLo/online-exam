package com.maghert.examsystem.controller;

import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examsystem.context.RequestContext;
import com.maghert.examsystem.context.RequestContextResolver;
import com.maghert.examsystem.model.vo.SystemLogExportView;
import com.maghert.examsystem.service.SystemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SystemControllerExportTests {

    private MockMvc mockMvc;
    private SystemService systemService;
    private RequestContextResolver requestContextResolver;

    @BeforeEach
    void setUp() {
        systemService = Mockito.mock(SystemService.class);
        requestContextResolver = Mockito.mock(RequestContextResolver.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SystemController(systemService, requestContextResolver)).build();
    }

    @Test
    void shouldReturnExportMetadata() throws Exception {
        Mockito.when(requestContextResolver.resolve(Mockito.any()))
                .thenReturn(new RequestContext(5001L, 5, "req-system-export"));
        Mockito.when(systemService.exportLogs(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(ApiResponse.ok(SystemLogExportView.builder()
                                .fileKey("system-log-export-001")
                                .fileName("system-log-export-20260421.csv")
                                .recordCount(1)
                                .generatedAt(LocalDateTime.of(2026, 4, 21, 10, 0))
                                .build())
                        .withRequestId("req-system-export"));

        mockMvc.perform(get("/api/system/log/export").param("logType", "config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("req-system-export"))
                .andExpect(jsonPath("$.data.fileKey").value("system-log-export-001"))
                .andExpect(jsonPath("$.data.recordCount").value(1));
    }
}
