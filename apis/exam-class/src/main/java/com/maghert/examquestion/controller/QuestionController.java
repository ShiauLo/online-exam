package com.maghert.examquestion.controller;

import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examcommon.web.PageResult;
import com.maghert.examquestion.dto.QuestionAuditRequest;
import com.maghert.examquestion.dto.QuestionCategoryQueryRequest;
import com.maghert.examquestion.dto.QuestionCategoryUpsertRequest;
import com.maghert.examquestion.dto.QuestionCreateRequest;
import com.maghert.examquestion.dto.QuestionDeleteRequest;
import com.maghert.examquestion.dto.QuestionQueryRequest;
import com.maghert.examquestion.dto.QuestionToggleStatusRequest;
import com.maghert.examquestion.dto.QuestionUpdateRequest;
import com.maghert.examquestion.service.QuestionService;
import com.maghert.examquestion.vo.QuestionCategoryView;
import com.maghert.examquestion.vo.QuestionExportView;
import com.maghert.examquestion.vo.QuestionImportResult;
import com.maghert.examquestion.vo.QuestionView;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
@RequestMapping("/api/question")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping("/create")
    public ApiResponse<QuestionView> create(@RequestBody @Valid QuestionCreateRequest request) throws BusinessException {
        return questionService.create(request);
    }

    @PutMapping("/update")
    public ApiResponse<QuestionView> update(@RequestBody @Valid QuestionUpdateRequest request) throws BusinessException {
        return questionService.update(request);
    }

    @DeleteMapping("/delete")
    public ApiResponse<String> delete(@RequestBody @Valid QuestionDeleteRequest request) throws BusinessException {
        return questionService.delete(request);
    }

    @PutMapping("/toggle-status")
    public ApiResponse<QuestionView> toggleStatus(@RequestBody @Valid QuestionToggleStatusRequest request)
            throws BusinessException {
        return questionService.toggleStatus(request);
    }

    @PostMapping("/category")
    public ApiResponse<QuestionCategoryView> createCategory(@RequestBody @Valid QuestionCategoryUpsertRequest request)
            throws BusinessException {
        return questionService.createCategory(request);
    }

    @PutMapping("/category")
    public ApiResponse<QuestionCategoryView> updateCategory(@RequestBody @Valid QuestionCategoryUpsertRequest request)
            throws BusinessException {
        return questionService.updateCategory(request);
    }

    @PostMapping("/category/query")
    public ApiResponse<PageResult<QuestionCategoryView>> queryCategories(
            @RequestBody @Valid QuestionCategoryQueryRequest request) throws BusinessException {
        return questionService.queryCategories(request);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<QuestionImportResult> importQuestions(@RequestPart("file") MultipartFile file,
                                                             @RequestParam("categoryId") Long categoryId)
            throws BusinessException {
        return questionService.importQuestions(file, categoryId);
    }

    @GetMapping("/export")
    public ApiResponse<QuestionExportView> exportQuestions(@RequestParam(value = "categoryId", required = false) Long categoryId,
                                                           @RequestParam(value = "creatorId", required = false) Long creatorId)
            throws BusinessException {
        return questionService.exportQuestions(categoryId, creatorId);
    }

    @PutMapping("/audit")
    public ApiResponse<QuestionView> audit(@RequestBody @Valid QuestionAuditRequest request) throws BusinessException {
        return questionService.audit(request);
    }

    @PostMapping("/query")
    public ApiResponse<PageResult<QuestionView>> query(@RequestBody @Valid QuestionQueryRequest request)
            throws BusinessException {
        return questionService.query(request);
    }
}
