package com.maghert.examscore.service;

import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examcommon.web.PageResult;
import com.maghert.examscore.context.RequestContext;
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

public interface ExamScoreService {

    ApiResponse<ScoreApplyRecheckView> applyRecheck(ScoreApplyRecheckRequest request, RequestContext context)
            throws BusinessException;

    ApiResponse<ScoreAnalyzeView> analyze(ScoreAnalyzeRequest request, RequestContext context) throws BusinessException;

    ApiResponse<ScoreAutoScoreView> autoScore(ScoreAutoScoreRequest request, RequestContext context)
            throws BusinessException;

    ApiResponse<PageResult<ScoreQueryView>> query(ScoreQueryRequest request, RequestContext context)
            throws BusinessException;

    ApiResponse<ScoreDetailView> detail(ScoreDetailRequest request, RequestContext context) throws BusinessException;

    ApiResponse<ScoreManualScoreView> manualScore(ScoreManualScoreRequest request, RequestContext context)
            throws BusinessException;

    ApiResponse<ScorePublishView> publish(ScorePublishRequest request, RequestContext context) throws BusinessException;

    ApiResponse<ScoreHandleAppealView> handleAppeal(ScoreHandleAppealRequest request, RequestContext context)
            throws BusinessException;

    ApiResponse<ScoreExportView> export(Long examId, Long classId, Boolean includeAnalysis, RequestContext context)
            throws BusinessException;

    ApiResponse<ScoreUpdateView> update(ScoreUpdateRequest request, RequestContext context) throws BusinessException;
}
