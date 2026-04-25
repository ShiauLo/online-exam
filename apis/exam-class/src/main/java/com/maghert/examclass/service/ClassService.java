package com.maghert.examclass.service;

import com.maghert.examclass.context.RequestContext;
import com.maghert.examclass.model.dto.ClassApplyJoinRequest;
import com.maghert.examclass.model.dto.ClassApproveJoinRequest;
import com.maghert.examclass.model.dto.ClassCreateRequest;
import com.maghert.examclass.model.dto.ClassDeleteRequest;
import com.maghert.examclass.model.dto.ClassQueryRequest;
import com.maghert.examclass.model.dto.ClassQuitRequest;
import com.maghert.examclass.model.dto.ClassRemoveStudentRequest;
import com.maghert.examclass.model.dto.ClassUpdateRequest;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.web.ApiResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ClassService {

    ApiResponse<?> create(ClassCreateRequest request, RequestContext context) throws BusinessException;

    ApiResponse<?> query(ClassQueryRequest request, RequestContext context);

    ApiResponse<?> update(ClassUpdateRequest request, RequestContext context) throws BusinessException;

    ApiResponse<?> delete(ClassDeleteRequest request, RequestContext context) throws BusinessException;

    ApiResponse<?> applyJoin(ClassApplyJoinRequest request, RequestContext context) throws BusinessException;

    ApiResponse<?> approveJoin(ClassApproveJoinRequest request, RequestContext context) throws BusinessException;

    ApiResponse<?> removeStudent(ClassRemoveStudentRequest request, RequestContext context) throws BusinessException;

    ApiResponse<?> quit(ClassQuitRequest request, RequestContext context) throws BusinessException;

    ApiResponse<?> importClasses(MultipartFile file, Long defaultTeacherId, RequestContext context) throws BusinessException;

    String export(Long classId, RequestContext context) throws BusinessException;
}
