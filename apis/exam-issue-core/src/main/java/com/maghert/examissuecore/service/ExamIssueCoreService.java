package com.maghert.examissuecore.service;

import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examcommon.web.PageResult;
import com.maghert.examissuecore.context.RequestContext;
import com.maghert.examissuecore.model.dto.IssueCloseRequest;
import com.maghert.examissuecore.model.dto.IssueCreateRequest;
import com.maghert.examissuecore.model.dto.IssueHandleRequest;
import com.maghert.examissuecore.model.dto.IssueQueryRequest;
import com.maghert.examissuecore.model.dto.IssueTrackRequest;
import com.maghert.examissuecore.model.dto.IssueTransferRequest;
import com.maghert.examissuecore.model.vo.IssueTrackView;
import com.maghert.examissuecore.model.vo.IssueView;

public interface ExamIssueCoreService {

    ApiResponse<IssueView> create(IssueCreateRequest request, RequestContext context) throws BusinessException;

    ApiResponse<PageResult<IssueView>> query(IssueQueryRequest request, RequestContext context) throws BusinessException;

    ApiResponse<IssueView> handle(IssueHandleRequest request, RequestContext context) throws BusinessException;

    ApiResponse<IssueView> transfer(IssueTransferRequest request, RequestContext context) throws BusinessException;

    ApiResponse<IssueView> close(IssueCloseRequest request, RequestContext context) throws BusinessException;

    ApiResponse<IssueTrackView> track(IssueTrackRequest request, RequestContext context) throws BusinessException;
}
