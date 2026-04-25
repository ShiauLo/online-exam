package com.maghert.examquestion.service;

import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examquestion.dto.QuestionAuditRequest;
import com.maghert.examquestion.dto.QuestionCategoryQueryRequest;
import com.maghert.examquestion.dto.QuestionCategoryUpsertRequest;
import com.maghert.examquestion.dto.QuestionCreateRequest;
import com.maghert.examquestion.dto.QuestionDeleteRequest;
import com.maghert.examquestion.dto.QuestionQueryRequest;
import com.maghert.examquestion.dto.QuestionToggleStatusRequest;
import com.maghert.examquestion.dto.QuestionUpdateRequest;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examcommon.web.PageResult;
import com.maghert.examquestion.vo.QuestionCategoryView;
import com.maghert.examquestion.vo.QuestionExportView;
import com.maghert.examquestion.vo.QuestionImportResult;
import com.maghert.examquestion.vo.QuestionView;
import org.springframework.web.multipart.MultipartFile;

public interface QuestionService {

    ApiResponse<QuestionView> create(QuestionCreateRequest request) throws BusinessException;

    ApiResponse<QuestionView> update(QuestionUpdateRequest request) throws BusinessException;

    ApiResponse<String> delete(QuestionDeleteRequest request) throws BusinessException;

    ApiResponse<QuestionView> toggleStatus(QuestionToggleStatusRequest request) throws BusinessException;

    ApiResponse<QuestionCategoryView> createCategory(QuestionCategoryUpsertRequest request) throws BusinessException;

    ApiResponse<QuestionCategoryView> updateCategory(QuestionCategoryUpsertRequest request) throws BusinessException;

    ApiResponse<QuestionImportResult> importQuestions(MultipartFile file, Long categoryId) throws BusinessException;

    ApiResponse<QuestionExportView> exportQuestions(Long categoryId, Long creatorId) throws BusinessException;

    ApiResponse<QuestionView> audit(QuestionAuditRequest request) throws BusinessException;

    ApiResponse<PageResult<QuestionView>> query(QuestionQueryRequest request) throws BusinessException;

    ApiResponse<PageResult<QuestionCategoryView>> queryCategories(QuestionCategoryQueryRequest request) throws BusinessException;
}
