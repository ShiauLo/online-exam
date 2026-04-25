package com.maghert.examcore.service;

import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examcommon.web.PageResult;
import com.maghert.examcore.context.RequestContext;
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

public interface ExamCoreService {

    ApiResponse<ExamView> create(ExamCreateRequest request, RequestContext context) throws BusinessException;

    ApiResponse<PageResult<ExamView>> query(ExamQueryRequest request, RequestContext context) throws BusinessException;

    ApiResponse<ExamView> updateParams(ExamUpdateParamsRequest request, RequestContext context) throws BusinessException;

    ApiResponse<ExamDistributeView> distribute(ExamDistributeRequest request, RequestContext context) throws BusinessException;

    ApiResponse<ExamSubmitView> submit(ExamSubmitRequest request, RequestContext context) throws BusinessException;

    ApiResponse<ExamRetestApplyView> applyRetest(ExamApplyRetestRequest request, RequestContext context)
            throws BusinessException;

    ApiResponse<ExamView> toggleStatus(ExamToggleStatusRequest request, RequestContext context) throws BusinessException;

    ApiResponse<String> approveRetest(ExamApproveRetestRequest request, RequestContext context) throws BusinessException;
}
