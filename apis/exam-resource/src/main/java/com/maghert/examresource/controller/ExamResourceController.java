package com.maghert.examresource.controller;

import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examresource.context.RequestContext;
import com.maghert.examresource.context.RequestContextResolver;
import com.maghert.examresource.model.vo.ResourceFileDownloadView;
import com.maghert.examresource.model.vo.ResourcePaperTemplateView;
import com.maghert.examresource.model.vo.ResourceQuestionImageView;
import com.maghert.examresource.service.ExamResourceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/resource")
public class ExamResourceController {

    private final ExamResourceService examResourceService;
    private final RequestContextResolver requestContextResolver;

    public ExamResourceController(ExamResourceService examResourceService,
                                  RequestContextResolver requestContextResolver) {
        this.examResourceService = examResourceService;
        this.requestContextResolver = requestContextResolver;
    }

    @GetMapping("/question/img")
    public ApiResponse<ResourceQuestionImageView> questionImage(@RequestParam("imgKey") @NotBlank(message = "imgKey 不能为空") String imgKey,
                                                                @RequestParam(value = "token", required = false) String token,
                                                                HttpServletRequest request) throws BusinessException {
        return examResourceService.questionImage(imgKey, token, contextOf(request));
    }

    @GetMapping("/paper/template")
    public ApiResponse<ResourcePaperTemplateView> paperTemplate(@RequestParam("paperType") @NotBlank(message = "paperType 不能为空") String paperType,
                                                                @RequestParam(value = "token", required = false) String token,
                                                                HttpServletRequest request) throws BusinessException {
        return examResourceService.paperTemplate(paperType, token, contextOf(request));
    }

    @GetMapping("/file/download")
    public ApiResponse<ResourceFileDownloadView> downloadFile(@RequestParam("fileKey") @NotBlank(message = "fileKey 不能为空") String fileKey,
                                                              @RequestParam(value = "token", required = false) String token,
                                                              HttpServletRequest request) throws BusinessException {
        return examResourceService.downloadFile(fileKey, token, contextOf(request));
    }

    private RequestContext contextOf(HttpServletRequest request) {
        return requestContextResolver.resolve(request);
    }
}
