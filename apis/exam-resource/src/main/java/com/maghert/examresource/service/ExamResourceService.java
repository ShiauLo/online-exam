package com.maghert.examresource.service;

import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examresource.context.RequestContext;
import com.maghert.examresource.model.vo.ResourceFileDownloadView;
import com.maghert.examresource.model.vo.ResourcePaperTemplateView;
import com.maghert.examresource.model.vo.ResourceQuestionImageView;

public interface ExamResourceService {

    ApiResponse<ResourceQuestionImageView> questionImage(String imgKey, String token, RequestContext context)
            throws BusinessException;

    ApiResponse<ResourcePaperTemplateView> paperTemplate(String paperType, String token, RequestContext context)
            throws BusinessException;

    ApiResponse<ResourceFileDownloadView> downloadFile(String fileKey, String token, RequestContext context)
            throws BusinessException;
}
