package com.maghert.exampaper.controller;

import com.maghert.examcommon.web.ApiResponse;
import com.maghert.exampaper.context.RequestContext;
import com.maghert.exampaper.context.RequestContextResolver;
import com.maghert.exampaper.model.vo.PaperExportView;
import com.maghert.exampaper.service.PaperService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaperControllerTests {

    private MockMvc mockMvc;
    private PaperService paperService;
    private RequestContextResolver requestContextResolver;

    @BeforeEach
    void setUp() {
        paperService = Mockito.mock(PaperService.class);
        requestContextResolver = Mockito.mock(RequestContextResolver.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PaperController(paperService, requestContextResolver)).build();
    }

    @Test
    void shouldReturnExportMetadata() throws Exception {
        Mockito.when(requestContextResolver.resolve(Mockito.any()))
                .thenReturn(new RequestContext(3001L, 3, "req-paper-export"));
        Mockito.when(paperService.export(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(ApiResponse.ok(PaperExportView.builder()
                                .fileKey("paper-export-001")
                                .fileName("paper-export-20260421.csv")
                                .recordCount(2)
                                .generatedAt(LocalDateTime.of(2026, 4, 21, 10, 0))
                                .build())
                        .withRequestId("req-paper-export"));

        mockMvc.perform(get("/api/paper/export").param("paperId", "9401"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("req-paper-export"))
                .andExpect(jsonPath("$.data.fileKey").value("paper-export-001"))
                .andExpect(jsonPath("$.data.recordCount").value(2));
    }
}
