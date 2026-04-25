package com.maghert.examscore.controller;

import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examcommon.web.PageResult;
import com.maghert.examscore.context.RequestContext;
import com.maghert.examscore.context.RequestContextResolver;
import com.maghert.examscore.model.dto.ScoreApplyRecheckRequest;
import com.maghert.examscore.model.dto.ScoreAutoScoreRequest;
import com.maghert.examscore.model.dto.ScoreAnalyzeRequest;
import com.maghert.examscore.model.dto.ScoreDetailRequest;
import com.maghert.examscore.model.dto.ScoreHandleAppealRequest;
import com.maghert.examscore.model.dto.ScoreManualScoreRequest;
import com.maghert.examscore.model.dto.ScorePublishRequest;
import com.maghert.examscore.model.dto.ScoreQueryRequest;
import com.maghert.examscore.model.dto.ScoreUpdateRequest;
import com.maghert.examscore.model.vo.ScoreApplyRecheckView;
import com.maghert.examscore.model.vo.ScoreAnalyzeView;
import com.maghert.examscore.model.vo.ScoreAutoScoreView;
import com.maghert.examscore.model.vo.ScoreDetailView;
import com.maghert.examscore.model.vo.ScoreExportView;
import com.maghert.examscore.model.vo.ScoreHandleAppealView;
import com.maghert.examscore.model.vo.ScoreManualScoreView;
import com.maghert.examscore.model.vo.ScorePublishView;
import com.maghert.examscore.model.vo.ScoreQueryView;
import com.maghert.examscore.model.vo.ScoreUpdateView;
import com.maghert.examscore.service.ExamScoreService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/score")
public class ExamScoreController {

    private final ExamScoreService examScoreService;
    private final RequestContextResolver requestContextResolver;

    public ExamScoreController(ExamScoreService examScoreService,
                               RequestContextResolver requestContextResolver) {
        this.examScoreService = examScoreService;
        this.requestContextResolver = requestContextResolver;
    }

    @PostMapping("/analyze")
    public ApiResponse<ScoreAnalyzeView> analyze(@RequestBody @Valid ScoreAnalyzeRequest request,
                                                 HttpServletRequest httpServletRequest)
            throws BusinessException {
        return examScoreService.analyze(request, contextOf(httpServletRequest));
    }

    @PostMapping("/apply-recheck")
    public ApiResponse<ScoreApplyRecheckView> applyRecheck(@RequestBody @Valid ScoreApplyRecheckRequest request,
                                                           HttpServletRequest httpServletRequest)
            throws BusinessException {
        return examScoreService.applyRecheck(request, contextOf(httpServletRequest));
    }

    @PostMapping("/auto-score")
    public ApiResponse<ScoreAutoScoreView> autoScore(@RequestBody @Valid ScoreAutoScoreRequest request,
                                                     HttpServletRequest httpServletRequest)
            throws BusinessException {
        return examScoreService.autoScore(request, contextOf(httpServletRequest));
    }

    @PostMapping("/query")
    public ApiResponse<PageResult<ScoreQueryView>> query(@RequestBody @Valid ScoreQueryRequest request,
                                                         HttpServletRequest httpServletRequest)
            throws BusinessException {
        return examScoreService.query(request, contextOf(httpServletRequest));
    }

    @PostMapping("/detail")
    public ApiResponse<ScoreDetailView> detail(@RequestBody @Valid ScoreDetailRequest request,
                                               HttpServletRequest httpServletRequest) throws BusinessException {
        return examScoreService.detail(request, contextOf(httpServletRequest));
    }

    @PutMapping("/manual-score")
    public ApiResponse<ScoreManualScoreView> manualScore(@RequestBody @Valid ScoreManualScoreRequest request,
                                                         HttpServletRequest httpServletRequest)
            throws BusinessException {
        return examScoreService.manualScore(request, contextOf(httpServletRequest));
    }

    @PutMapping("/publish")
    public ApiResponse<ScorePublishView> publish(@RequestBody @Valid ScorePublishRequest request,
                                                 HttpServletRequest httpServletRequest) throws BusinessException {
        return examScoreService.publish(request, contextOf(httpServletRequest));
    }

    @PutMapping("/handle-appeal")
    public ApiResponse<ScoreHandleAppealView> handleAppeal(@RequestBody @Valid ScoreHandleAppealRequest request,
                                                           HttpServletRequest httpServletRequest)
            throws BusinessException {
        return examScoreService.handleAppeal(request, contextOf(httpServletRequest));
    }

    @GetMapping("/export")
    public ApiResponse<ScoreExportView> export(@RequestParam(value = "examId", required = false) Long examId,
                                               @RequestParam(value = "classId", required = false) Long classId,
                                               @RequestParam(value = "includeAnalysis", required = false) Boolean includeAnalysis,
                                               HttpServletRequest httpServletRequest) throws BusinessException {
        return examScoreService.export(examId, classId, includeAnalysis, contextOf(httpServletRequest));
    }

    @PutMapping("/update")
    public ApiResponse<ScoreUpdateView> update(@RequestBody @Valid ScoreUpdateRequest request,
                                               HttpServletRequest httpServletRequest) throws BusinessException {
        return examScoreService.update(request, contextOf(httpServletRequest));
    }

    private RequestContext contextOf(HttpServletRequest request) {
        return requestContextResolver.resolve(request);
    }
}
