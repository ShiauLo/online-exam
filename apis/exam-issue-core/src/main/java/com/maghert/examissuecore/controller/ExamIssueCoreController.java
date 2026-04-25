package com.maghert.examissuecore.controller;

import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examcommon.web.PageResult;
import com.maghert.examissuecore.context.RequestContext;
import com.maghert.examissuecore.context.RequestContextResolver;
import com.maghert.examissuecore.model.dto.IssueCloseRequest;
import com.maghert.examissuecore.model.dto.IssueCreateRequest;
import com.maghert.examissuecore.model.dto.IssueHandleRequest;
import com.maghert.examissuecore.model.dto.IssueQueryRequest;
import com.maghert.examissuecore.model.dto.IssueTrackRequest;
import com.maghert.examissuecore.model.dto.IssueTransferRequest;
import com.maghert.examissuecore.model.vo.IssueTrackView;
import com.maghert.examissuecore.model.vo.IssueView;
import com.maghert.examissuecore.service.ExamIssueCoreService;
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
@RequestMapping("/api/issue/core")
public class ExamIssueCoreController {

    private final ExamIssueCoreService examIssueCoreService;
    private final RequestContextResolver requestContextResolver;

    public ExamIssueCoreController(ExamIssueCoreService examIssueCoreService,
                                   RequestContextResolver requestContextResolver) {
        this.examIssueCoreService = examIssueCoreService;
        this.requestContextResolver = requestContextResolver;
    }

    @PostMapping("/create")
    public ApiResponse<IssueView> create(@RequestBody @Valid IssueCreateRequest request,
                                         HttpServletRequest httpServletRequest) throws BusinessException {
        return examIssueCoreService.create(request, contextOf(httpServletRequest));
    }

    @PostMapping("/query")
    public ApiResponse<PageResult<IssueView>> query(@RequestBody @Valid IssueQueryRequest request,
                                                    HttpServletRequest httpServletRequest) throws BusinessException {
        return examIssueCoreService.query(request, contextOf(httpServletRequest));
    }

    @PutMapping("/handle")
    public ApiResponse<IssueView> handle(@RequestBody @Valid IssueHandleRequest request,
                                         HttpServletRequest httpServletRequest) throws BusinessException {
        return examIssueCoreService.handle(request, contextOf(httpServletRequest));
    }

    @PutMapping("/transfer")
    public ApiResponse<IssueView> transfer(@RequestBody @Valid IssueTransferRequest request,
                                           HttpServletRequest httpServletRequest) throws BusinessException {
        return examIssueCoreService.transfer(request, contextOf(httpServletRequest));
    }

    @PutMapping("/close")
    public ApiResponse<IssueView> close(@RequestBody @Valid IssueCloseRequest request,
                                        HttpServletRequest httpServletRequest) throws BusinessException {
        return examIssueCoreService.close(request, contextOf(httpServletRequest));
    }

    @PostMapping("/track")
    public ApiResponse<IssueTrackView> track(@RequestBody @Valid IssueTrackRequest request,
                                             HttpServletRequest httpServletRequest) throws BusinessException {
        return examIssueCoreService.track(request, contextOf(httpServletRequest));
    }

    private RequestContext contextOf(HttpServletRequest request) {
        return requestContextResolver.resolve(request);
    }
}
