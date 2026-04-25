package com.maghert.examsystem.service;

import com.maghert.examsystem.context.RequestContext;
import com.maghert.examsystem.model.dto.AlarmQueryRequest;
import com.maghert.examsystem.model.dto.AlarmSettingRequest;
import com.maghert.examsystem.model.dto.DataBackupRequest;
import com.maghert.examsystem.model.dto.DataQueryRequest;
import com.maghert.examsystem.model.dto.DataRestoreRequest;
import com.maghert.examsystem.model.dto.PermissionAssignRequest;
import com.maghert.examsystem.model.dto.PermissionQueryRequest;
import com.maghert.examsystem.model.dto.RoleUpsertRequest;
import com.maghert.examsystem.model.dto.SystemConfigQueryRequest;
import com.maghert.examsystem.model.dto.SystemConfigUpdateRequest;
import com.maghert.examsystem.model.dto.SystemLogQueryRequest;
import com.maghert.examsystem.model.vo.SystemLogExportView;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.web.ApiResponse;

public interface SystemService {

    ApiResponse<?> queryPermissions(PermissionQueryRequest request, RequestContext context) throws BusinessException;

    ApiResponse<?> upsertRole(RoleUpsertRequest request, RequestContext context) throws BusinessException;

    ApiResponse<?> assignPermission(PermissionAssignRequest request, RequestContext context) throws BusinessException;

    ApiResponse<?> queryConfigs(SystemConfigQueryRequest request, RequestContext context) throws BusinessException;

    ApiResponse<?> updateConfig(SystemConfigUpdateRequest request, RequestContext context) throws BusinessException;

    ApiResponse<?> queryAlarms(AlarmQueryRequest request, RequestContext context) throws BusinessException;

    ApiResponse<?> updateAlarmSetting(AlarmSettingRequest request, RequestContext context) throws BusinessException;

    ApiResponse<?> queryLogs(SystemLogQueryRequest request, RequestContext context) throws BusinessException;

    ApiResponse<SystemLogExportView> exportLogs(
            String logType,
            String startTime,
            String endTime,
            String approverId,
            RequestContext context)
            throws BusinessException;

    ApiResponse<?> queryBackups(DataQueryRequest request, RequestContext context) throws BusinessException;

    ApiResponse<?> backup(DataBackupRequest request, RequestContext context) throws BusinessException;

    ApiResponse<?> restore(DataRestoreRequest request, RequestContext context) throws BusinessException;
}
