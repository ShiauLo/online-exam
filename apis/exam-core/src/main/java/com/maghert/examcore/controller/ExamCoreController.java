package com.maghert.examcore.controller;

import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examcommon.web.PageResult;
import com.maghert.examcore.context.RequestContext;
import com.maghert.examcore.context.RequestContextResolver;
import com.maghert.examcore.model.dto.ExamApplyRetestRequest;
import com.maghert.examcore.model.dto.ExamApproveRetestRequest;
import com.maghert.examcore.model.dto.ExamCreateRequest;
import com.maghert.examcore.model.dto.ExamDistributeRequest;
import com.maghert.examcore.model.dto.ExamQueryRequest;
import com.maghert.examcore.model.dto.ExamSubmitRequest;
import com.maghert.examcore.model.dto.ExamToggleStatusRequest;
import com.maghert.examcore.model.dto.ExamUpdateParamsRequest;
import com.maghert.examcore.model.vo.ExamDistributeView;
import com.maghert.examcore.model.vo.ExamRetestApplyView;
import com.maghert.examcore.model.vo.ExamSubmitView;
import com.maghert.examcore.model.vo.ExamView;
import com.maghert.examcore.service.ExamCoreService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/exam/core")
public class ExamCoreController {

    private final ExamCoreService examCoreService;
    private final RequestContextResolver requestContextResolver;

    public ExamCoreController(ExamCoreService examCoreService, RequestContextResolver requestContextResolver) {
        this.examCoreService = examCoreService;
        this.requestContextResolver = requestContextResolver;
    }

    @PostMapping("/create")
    public ApiResponse<ExamView> create(@RequestBody @Valid ExamCreateRequest request,
                                        HttpServletRequest httpServletRequest) throws BusinessException {
        return examCoreService.create(request, contextOf(httpServletRequest));
    }

    @PostMapping("/query")
    public ApiResponse<PageResult<ExamView>> query(@RequestBody @Valid ExamQueryRequest request,
                                                   HttpServletRequest httpServletRequest) throws BusinessException {
        return examCoreService.query(request, contextOf(httpServletRequest));
    }

    @PutMapping("/update/params")
    public ApiResponse<ExamView> updateParams(@RequestBody @Valid ExamUpdateParamsRequest request,
                                              HttpServletRequest httpServletRequest) throws BusinessException {
        return examCoreService.updateParams(request, contextOf(httpServletRequest));
    }

    @PutMapping("/distribute")
    public ApiResponse<ExamDistributeView> distribute(@RequestBody @Valid ExamDistributeRequest request,
                                                      HttpServletRequest httpServletRequest) throws BusinessException {
        return examCoreService.distribute(request, contextOf(httpServletRequest));
    }

    @PutMapping("/submit")
    public ApiResponse<ExamSubmitView> submit(@RequestBody @Valid ExamSubmitRequest request,
                                              HttpServletRequest httpServletRequest) throws BusinessException {
        return examCoreService.submit(request, contextOf(httpServletRequest));
    }

    @PostMapping("/apply-retest")
    public ApiResponse<ExamRetestApplyView> applyRetest(@RequestBody @Valid ExamApplyRetestRequest request,
                                                        HttpServletRequest httpServletRequest)
            throws BusinessException {
        return examCoreService.applyRetest(request, contextOf(httpServletRequest));
    }

    @PutMapping("/toggle-status")
    public ApiResponse<ExamView> toggleStatus(@RequestBody @Valid ExamToggleStatusRequest request,
                                              HttpServletRequest httpServletRequest) throws BusinessException {
        return examCoreService.toggleStatus(request, contextOf(httpServletRequest));
    }

    @PutMapping("/approve-retest")
    public ApiResponse<String> approveRetest(@RequestBody @Valid ExamApproveRetestRequest request,
                                             HttpServletRequest httpServletRequest) throws BusinessException {
        return examCoreService.approveRetest(request, contextOf(httpServletRequest));
    }

    private RequestContext contextOf(HttpServletRequest request) {
        return requestContextResolver.resolve(request);
    }
}
