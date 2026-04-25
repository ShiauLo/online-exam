package com.maghert.examclass.controller;

import com.maghert.examclass.context.RequestContext;
import com.maghert.examclass.context.RequestContextResolver;
import com.maghert.examclass.model.dto.ClassApplyJoinRequest;
import com.maghert.examclass.model.dto.ClassApproveJoinRequest;
import com.maghert.examclass.model.dto.ClassCreateRequest;
import com.maghert.examclass.model.dto.ClassDeleteRequest;
import com.maghert.examclass.model.dto.ClassQueryRequest;
import com.maghert.examclass.model.dto.ClassQuitRequest;
import com.maghert.examclass.model.dto.ClassRemoveStudentRequest;
import com.maghert.examclass.model.dto.ClassUpdateRequest;
import com.maghert.examclass.service.ClassService;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/class")
public class ClassController {

    private final ClassService classService;
    private final RequestContextResolver requestContextResolver;

    public ClassController(ClassService classService, RequestContextResolver requestContextResolver) {
        this.classService = classService;
        this.requestContextResolver = requestContextResolver;
    }

    @PostMapping("/create")
    public ApiResponse<?> create(@RequestBody @Valid ClassCreateRequest request, HttpServletRequest httpServletRequest)
            throws BusinessException {
        return classService.create(request, contextOf(httpServletRequest));
    }

    @PostMapping("/query")
    public ApiResponse<?> query(@RequestBody @Valid ClassQueryRequest request, HttpServletRequest httpServletRequest) {
        return classService.query(request, contextOf(httpServletRequest));
    }

    @PutMapping("/update")
    public ApiResponse<?> update(@RequestBody @Valid ClassUpdateRequest request, HttpServletRequest httpServletRequest)
            throws BusinessException {
        return classService.update(request, contextOf(httpServletRequest));
    }

    @DeleteMapping("/delete")
    public ApiResponse<?> delete(@RequestBody @Valid ClassDeleteRequest request, HttpServletRequest httpServletRequest)
            throws BusinessException {
        return classService.delete(request, contextOf(httpServletRequest));
    }

    @PostMapping("/apply-join")
    public ApiResponse<?> applyJoin(@RequestBody @Valid ClassApplyJoinRequest request, HttpServletRequest httpServletRequest)
            throws BusinessException {
        return classService.applyJoin(request, contextOf(httpServletRequest));
    }

    @PutMapping("/approve-join")
    public ApiResponse<?> approveJoin(@RequestBody @Valid ClassApproveJoinRequest request, HttpServletRequest httpServletRequest)
            throws BusinessException {
        return classService.approveJoin(request, contextOf(httpServletRequest));
    }

    @PutMapping("/remove-student")
    public ApiResponse<?> removeStudent(
            @RequestBody @Valid ClassRemoveStudentRequest request,
            HttpServletRequest httpServletRequest) throws BusinessException {
        return classService.removeStudent(request, contextOf(httpServletRequest));
    }

    @PutMapping("/quit")
    public ApiResponse<?> quit(@RequestBody @Valid ClassQuitRequest request, HttpServletRequest httpServletRequest)
            throws BusinessException {
        return classService.quit(request, contextOf(httpServletRequest));
    }

    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<?> importClasses(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "defaultTeacherId", required = false) Long defaultTeacherId,
            HttpServletRequest httpServletRequest) throws BusinessException {
        return classService.importClasses(file, defaultTeacherId, contextOf(httpServletRequest));
    }

    @GetMapping("/export")
    public ResponseEntity<String> export(
            @RequestParam(value = "classId", required = false) Long classId,
            HttpServletRequest httpServletRequest) throws BusinessException {
        String csv = classService.export(classId, contextOf(httpServletRequest));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8")
                .body(csv);
    }

    private RequestContext contextOf(HttpServletRequest request) {
        return requestContextResolver.resolve(request);
    }
}
