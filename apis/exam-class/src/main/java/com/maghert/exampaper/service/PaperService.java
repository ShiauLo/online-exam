package com.maghert.exampaper.service;

import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examcommon.web.PageResult;
import com.maghert.exampaper.context.RequestContext;
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

public interface PaperService {

    ApiResponse<PageResult<PaperView>> query(PaperQueryRequest request, RequestContext context) throws BusinessException;

    ApiResponse<PaperView> createManual(PaperManualCreateRequest request, RequestContext context) throws BusinessException;

    ApiResponse<PaperView> createAuto(PaperAutoCreateRequest request, RequestContext context) throws BusinessException;

    ApiResponse<PaperView> update(PaperUpdateRequest request, RequestContext context) throws BusinessException;

    ApiResponse<String> delete(PaperDeleteRequest request, RequestContext context) throws BusinessException;

    ApiResponse<PaperView> audit(PaperAuditRequest request, RequestContext context) throws BusinessException;

    ApiResponse<PaperView> publish(PaperPublishRequest request, RequestContext context) throws BusinessException;

    ApiResponse<PaperView> terminate(PaperTerminateRequest request, RequestContext context) throws BusinessException;

    ApiResponse<PaperView> recycle(PaperRecycleRequest request, RequestContext context) throws BusinessException;

    ApiResponse<PaperExportView> export(Long paperId, String approverId, RequestContext context) throws BusinessException;
}
