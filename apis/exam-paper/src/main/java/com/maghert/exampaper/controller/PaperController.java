package com.maghert.exampaper.controller;

import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examcommon.web.PageResult;
import com.maghert.exampaper.context.RequestContext;
import com.maghert.exampaper.context.RequestContextResolver;
import com.maghert.exampaper.model.dto.PaperAuditRequest;
import com.maghert.exampaper.model.dto.PaperAutoCreateRequest;
import com.maghert.exampaper.model.dto.PaperDeleteRequest;
import com.maghert.exampaper.model.dto.PaperManualCreateRequest;
import com.maghert.exampaper.model.dto.PaperPublishRequest;
import com.maghert.exampaper.model.dto.PaperQueryRequest;
import com.maghert.exampaper.model.dto.PaperRecycleRequest;
import com.maghert.exampaper.model.dto.PaperTerminateRequest;
import com.maghert.exampaper.model.dto.PaperUpdateRequest;
import com.maghert.exampaper.model.vo.PaperExportView;
import com.maghert.exampaper.model.vo.PaperView;
import com.maghert.exampaper.service.PaperService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/paper")
public class PaperController {

    private final PaperService paperService;
    private final RequestContextResolver requestContextResolver;

    public PaperController(PaperService paperService, RequestContextResolver requestContextResolver) {
        this.paperService = paperService;
        this.requestContextResolver = requestContextResolver;
    }

    @PostMapping("/query")
    public ApiResponse<PageResult<PaperView>> query(@RequestBody @Valid PaperQueryRequest request,
                                                    HttpServletRequest httpServletRequest) throws BusinessException {
        return paperService.query(request, contextOf(httpServletRequest));
    }

    @GetMapping("/export")
    public ApiResponse<PaperExportView> export(@RequestParam(value = "paperId", required = false) Long paperId,
                                               @RequestParam(value = "approverId", required = false) String approverId,
                                               HttpServletRequest httpServletRequest) throws BusinessException {
        return paperService.export(paperId, approverId, contextOf(httpServletRequest));
    }

    @PostMapping("/create/manual")
    public ApiResponse<PaperView> createManual(@RequestBody @Valid PaperManualCreateRequest request,
                                               HttpServletRequest httpServletRequest) throws BusinessException {
        return paperService.createManual(request, contextOf(httpServletRequest));
    }

    @PostMapping("/create/auto")
    public ApiResponse<PaperView> createAuto(@RequestBody @Valid PaperAutoCreateRequest request,
                                             HttpServletRequest httpServletRequest) throws BusinessException {
        return paperService.createAuto(request, contextOf(httpServletRequest));
    }

    @PutMapping("/update")
    public ApiResponse<PaperView> update(@RequestBody @Valid PaperUpdateRequest request,
                                         HttpServletRequest httpServletRequest) throws BusinessException {
        return paperService.update(request, contextOf(httpServletRequest));
    }

    @DeleteMapping("/delete")
    public ApiResponse<String> delete(@RequestBody @Valid PaperDeleteRequest request,
                                      HttpServletRequest httpServletRequest) throws BusinessException {
        return paperService.delete(request, contextOf(httpServletRequest));
    }

    @PutMapping("/audit")
    public ApiResponse<PaperView> audit(@RequestBody @Valid PaperAuditRequest request,
                                        HttpServletRequest httpServletRequest) throws BusinessException {
        return paperService.audit(request, contextOf(httpServletRequest));
    }

    @PutMapping("/publish")
    public ApiResponse<PaperView> publish(@RequestBody @Valid PaperPublishRequest request,
                                          HttpServletRequest httpServletRequest) throws BusinessException {
        return paperService.publish(request, contextOf(httpServletRequest));
    }

    @PutMapping("/terminate")
    public ApiResponse<PaperView> terminate(@RequestBody @Valid PaperTerminateRequest request,
                                            HttpServletRequest httpServletRequest) throws BusinessException {
        return paperService.terminate(request, contextOf(httpServletRequest));
    }

    @PutMapping("/recycle")
    public ApiResponse<PaperView> recycle(@RequestBody @Valid PaperRecycleRequest request,
                                          HttpServletRequest httpServletRequest) throws BusinessException {
        return paperService.recycle(request, contextOf(httpServletRequest));
    }

    private RequestContext contextOf(HttpServletRequest request) {
        return requestContextResolver.resolve(request);
    }
}
