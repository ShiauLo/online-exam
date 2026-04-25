package com.maghert.examsystem.controller;

import com.maghert.examsystem.context.RequestContext;
import com.maghert.examsystem.context.RequestContextResolver;
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
import com.maghert.examsystem.service.SystemService;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/system")
public class SystemController {

    private final SystemService systemService;
    private final RequestContextResolver requestContextResolver;

    public SystemController(SystemService systemService, RequestContextResolver requestContextResolver) {
        this.systemService = systemService;
        this.requestContextResolver = requestContextResolver;
    }

    @PostMapping("/permission/query")
    public ApiResponse<?> queryPermissions(@RequestBody PermissionQueryRequest request, HttpServletRequest httpServletRequest)
            throws BusinessException {
        return systemService.queryPermissions(request, contextOf(httpServletRequest));
    }

    @PostMapping("/role")
    public ApiResponse<?> createRole(@RequestBody @Valid RoleUpsertRequest request, HttpServletRequest httpServletRequest)
            throws BusinessException {
        return systemService.upsertRole(request, contextOf(httpServletRequest));
    }

    @PutMapping("/role")
    public ApiResponse<?> updateRole(@RequestBody @Valid RoleUpsertRequest request, HttpServletRequest httpServletRequest)
            throws BusinessException {
        return systemService.upsertRole(request, contextOf(httpServletRequest));
    }

    @PutMapping("/permission/assign")
    public ApiResponse<?> assignPermission(
            @RequestBody @Valid PermissionAssignRequest request,
            HttpServletRequest httpServletRequest) throws BusinessException {
        return systemService.assignPermission(request, contextOf(httpServletRequest));
    }

    @PostMapping("/config/query")
    public ApiResponse<?> queryConfigs(
            @RequestBody @Valid SystemConfigQueryRequest request,
            HttpServletRequest httpServletRequest) throws BusinessException {
        return systemService.queryConfigs(request, contextOf(httpServletRequest));
    }

    @PutMapping("/config/update")
    public ApiResponse<?> updateConfig(
            @RequestBody @Valid SystemConfigUpdateRequest request,
            HttpServletRequest httpServletRequest) throws BusinessException {
        return systemService.updateConfig(request, contextOf(httpServletRequest));
    }

    @PostMapping("/alarm/query")
    public ApiResponse<?> queryAlarms(@RequestBody @Valid AlarmQueryRequest request, HttpServletRequest httpServletRequest)
            throws BusinessException {
        return systemService.queryAlarms(request, contextOf(httpServletRequest));
    }

    @PutMapping("/alarm/setting")
    public ApiResponse<?> updateAlarmSetting(
            @RequestBody @Valid AlarmSettingRequest request,
            HttpServletRequest httpServletRequest) throws BusinessException {
        return systemService.updateAlarmSetting(request, contextOf(httpServletRequest));
    }

    @PostMapping("/log/query")
    public ApiResponse<?> queryLogs(@RequestBody @Valid SystemLogQueryRequest request, HttpServletRequest httpServletRequest)
            throws BusinessException {
        return systemService.queryLogs(request, contextOf(httpServletRequest));
    }

    @GetMapping("/log/export")
    public ApiResponse<SystemLogExportView> exportLogs(
            @RequestParam(value = "logType", required = false) String logType,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @RequestParam(value = "approverId", required = false) String approverId,
            HttpServletRequest httpServletRequest) throws BusinessException {
        return systemService.exportLogs(logType, startTime, endTime, approverId, contextOf(httpServletRequest));
    }

    @PostMapping("/data/query")
    public ApiResponse<?> queryBackups(@RequestBody @Valid DataQueryRequest request, HttpServletRequest httpServletRequest)
            throws BusinessException {
        return systemService.queryBackups(request, contextOf(httpServletRequest));
    }

    @PostMapping("/data/backup")
    public ApiResponse<?> backup(@RequestBody @Valid DataBackupRequest request, HttpServletRequest httpServletRequest)
            throws BusinessException {
        return systemService.backup(request, contextOf(httpServletRequest));
    }

    @PostMapping("/data/restore")
    public ApiResponse<?> restore(@RequestBody @Valid DataRestoreRequest request, HttpServletRequest httpServletRequest)
            throws BusinessException {
        return systemService.restore(request, contextOf(httpServletRequest));
    }

    private RequestContext contextOf(HttpServletRequest request) {
        return requestContextResolver.resolve(request);
    }
}
