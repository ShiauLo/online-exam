package com.maghert.examsystem.service.impl;

import cn.hutool.core.lang.Snowflake;
import com.maghert.examcommon.audit.AuditTrail;
import com.maghert.examcommon.auth.RoleMappings;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.exception.DomainErrorCode;
import com.maghert.examcommon.resource.LocalResourceFileStore;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examcommon.web.PageResult;
import com.maghert.examsystem.context.RequestContext;
import com.maghert.examsystem.entity.SystemAlarmSettingEntity;
import com.maghert.examsystem.entity.SystemAuditRecordEntity;
import com.maghert.examsystem.entity.SystemBackupRecordEntity;
import com.maghert.examsystem.entity.SystemConfigEntity;
import com.maghert.examsystem.entity.SystemLogEntity;
import com.maghert.examsystem.entity.SystemPermissionAssignmentEntity;
import com.maghert.examsystem.entity.SystemRoleEntity;
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
import com.maghert.examsystem.model.enums.BackupTaskStatus;
import com.maghert.examsystem.model.vo.AlarmSettingVO;
import com.maghert.examsystem.model.vo.BackupRecordVO;
import com.maghert.examsystem.model.vo.BackupTaskAcceptedVO;
import com.maghert.examsystem.model.vo.MenuPermissionVO;
import com.maghert.examsystem.model.vo.PermissionQueryVO;
import com.maghert.examsystem.model.vo.RoleViewVO;
import com.maghert.examsystem.model.vo.RoutePermissionVO;
import com.maghert.examsystem.model.vo.SystemConfigVO;
import com.maghert.examsystem.model.vo.SystemLogExportView;
import com.maghert.examsystem.model.vo.SystemLogVO;
import com.maghert.examsystem.notification.SystemNotificationEvent;
import com.maghert.examsystem.notification.SystemNotificationPublisher;
import com.maghert.examsystem.repository.SystemDomainRepository;
import com.maghert.examsystem.service.SystemService;
import com.maghert.examsystem.task.SystemMaintenanceTaskRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class SystemServiceImpl implements SystemService {

    private static final String CONFIG_CONFIRM_CODE = "CONFIRM_SYSTEM_CONFIG";
    private static final String RESTORE_CONFIRM_CODE = "CONFIRM_SYSTEM_RESTORE";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> SUPPORTED_BACKUP_TYPES = List.of("full", "incremental", "config", "audit");
    private static final Set<String> SENSITIVE_CONFIG_KEYWORDS = Set.of("password", "secret", "token", "credential", "key");

    private final Snowflake snowflake;
    private final SystemDomainRepository repository;
    private final TaskExecutor taskExecutor;
    private final SystemNotificationPublisher notificationPublisher;
    private final SystemMaintenanceTaskRunner maintenanceTaskRunner;
    private final LocalResourceFileStore resourceFileStore;

    public SystemServiceImpl(Snowflake snowflake,
                             SystemDomainRepository repository,
                             @Qualifier("systemTaskExecutor") TaskExecutor taskExecutor,
                             SystemNotificationPublisher notificationPublisher,
                             SystemMaintenanceTaskRunner maintenanceTaskRunner,
                             @Value("${exam.resource.local-storage-root:./tmp_resource}") String localStorageRoot) {
        this.snowflake = snowflake;
        this.repository = repository;
        this.taskExecutor = taskExecutor;
        this.notificationPublisher = notificationPublisher;
        this.maintenanceTaskRunner = maintenanceTaskRunner;
        this.resourceFileStore = new LocalResourceFileStore(localStorageRoot);
    }

    @Override
    public ApiResponse<?> queryPermissions(PermissionQueryRequest request, RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        Integer effectiveRoleId = context.roleId();
        if (request.getRoleType() != null && RoleMappings.isAdmin(context.roleId())) {
            effectiveRoleId = switch (request.getRoleType()) {
                case "super_admin" -> 1;
                case "admin" -> 2;
                case "teacher" -> 3;
                case "student" -> 4;
                case "auditor" -> 5;
                case "ops" -> 6;
                default -> context.roleId();
            };
        }
        PermissionQueryVO payload = switch (effectiveRoleId) {
            case 1 -> buildSuperAdminPermissions();
            case 2 -> buildAdminPermissions();
            case 3 -> buildTeacherPermissions();
            case 4 -> buildStudentPermissions();
            case 5 -> buildAuditorPermissions();
            case 6 -> buildOpsPermissions();
            default -> throw new BusinessException(DomainErrorCode.SYSTEM_PERMISSION_QUERY_FORBIDDEN);
        };
        return ApiResponse.ok(payload).withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<?> upsertRole(RoleUpsertRequest request, RequestContext context) throws BusinessException {
        ensureSuperAdmin(context);
        SystemRoleEntity existingRole = request.getRoleId() == null ? null : repository.findRoleById(request.getRoleId()).orElse(null);
        String oldRoleName = existingRole == null ? null : existingRole.getRoleName();
        Long oldTemplateId = existingRole == null ? null : existingRole.getTemplateId();
        List<String> oldPermissionIds = existingRole == null ? null : existingRole.getPermissionIds();
        SystemRoleEntity role = request.getRoleId() == null
                ? new SystemRoleEntity().setRoleId(snowflake.nextId()).setCreateTime(LocalDateTime.now())
                : existingRole != null
                ? existingRole
                : new SystemRoleEntity().setRoleId(request.getRoleId()).setCreateTime(LocalDateTime.now());
        role.setRoleName(request.getRoleName())
                .setPermissionIds(request.getPermissionIds())
                .setTemplateId(request.getTemplateId())
                .setCreatedBy(context.userId())
                .setUpdateTime(LocalDateTime.now());
        if (existingRole == null) {
            repository.saveRole(role);
        } else {
            repository.updateRole(role);
        }
        saveLog("role", context, "upsert role " + role.getRoleName(), null);
        saveAudit(AuditTrail.builder()
                .actionType("role.upsert")
                .operatorId(context.userId())
                .targetType("role")
                .targetId(String.valueOf(role.getRoleId()))
                .requestId(context.requestId())
                .detail(existingRole == null
                        ? "created roleName=%s;templateId=%s;permissionIds=%s".formatted(
                        role.getRoleName(), role.getTemplateId(), role.getPermissionIds())
                        : "updated roleName=%s->%s;templateId=%s->%s;permissionIds=%s->%s".formatted(
                        oldRoleName, role.getRoleName(), oldTemplateId, role.getTemplateId(), oldPermissionIds, role.getPermissionIds()))
                .occurredAt(LocalDateTime.now())
                .build());

        return ApiResponse.ok(RoleViewVO.builder()
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .permissionIds(role.getPermissionIds())
                .templateId(role.getTemplateId())
                .build()).withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<?> assignPermission(PermissionAssignRequest request, RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        if (!RoleMappings.isAdmin(context.roleId())) {
            throw new BusinessException(DomainErrorCode.SYSTEM_OPERATION_FORBIDDEN);
        }
        if (Integer.valueOf(1).equals(request.getRoleId()) && !Integer.valueOf(1).equals(context.roleId())) {
            throw new BusinessException(DomainErrorCode.PERMISSION_ASSIGNMENT_FORBIDDEN);
        }
        SystemPermissionAssignmentEntity existingAssignment = repository.findAssignmentByAccountId(request.getAccountId()).orElse(null);
        Integer oldRoleId = existingAssignment == null ? null : existingAssignment.getRoleId();
        String oldExpireTime = existingAssignment == null ? null : existingAssignment.getExpireTime();
        SystemPermissionAssignmentEntity assignment = existingAssignment != null
                ? existingAssignment
                : new SystemPermissionAssignmentEntity()
                .setAssignmentId(snowflake.nextId())
                .setAccountId(request.getAccountId())
                .setCreateTime(LocalDateTime.now());
        assignment.setRoleId(request.getRoleId())
                .setExpireTime(request.getExpireTime())
                .setAssignedBy(context.userId())
                .setUpdateTime(LocalDateTime.now());
        if (existingAssignment != null) {
            repository.updateAssignment(assignment);
        } else {
            repository.saveAssignment(assignment);
        }
        saveLog("permission", context, "assign role " + request.getRoleId() + " to account " + request.getAccountId(), null);
        saveAudit(AuditTrail.builder()
                .actionType("permission.assign")
                .operatorId(context.userId())
                .targetType("account")
                .targetId(String.valueOf(request.getAccountId()))
                .requestId(context.requestId())
                .detail(existingAssignment == null
                        ? "assigned roleId=%s;expireTime=%s".formatted(assignment.getRoleId(), assignment.getExpireTime())
                        : "reassigned roleId=%s->%s;expireTime=%s->%s".formatted(
                        oldRoleId, assignment.getRoleId(), oldExpireTime, assignment.getExpireTime()))
                .occurredAt(LocalDateTime.now())
                .build());
        return ApiResponse.ok("ok").withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<?> queryConfigs(SystemConfigQueryRequest request, RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        List<SystemConfigVO> records = repository.listConfigs().stream()
                .filter(item -> request.getConfigKey() == null || item.getConfigKey().contains(request.getConfigKey()))
                .filter(item -> request.getCategory() == null || request.getCategory().equalsIgnoreCase(item.getCategory()))
                .sorted((left, right) -> right.getUpdateTime().compareTo(left.getUpdateTime()))
                .map(item -> SystemConfigVO.builder()
                        .configId(item.getConfigId())
                        .configKey(item.getConfigKey())
                        .configValue(item.getConfigValue())
                        .category(item.getCategory())
                        .build())
                .toList();
        return ApiResponse.ok(page(records, request.getPageNum(), request.getPageSize()))
                .withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<?> updateConfig(SystemConfigUpdateRequest request, RequestContext context) throws BusinessException {
        ensureSuperAdmin(context);
        ensureSecondFactor(request.getVerifyCode(), CONFIG_CONFIRM_CODE);
        SystemConfigEntity existingConfig = repository.findConfigByKey(request.getConfigKey()).orElse(null);
        String oldConfigValue = existingConfig == null ? null : existingConfig.getConfigValue();
        SystemConfigEntity config = existingConfig != null
                ? existingConfig
                : new SystemConfigEntity()
                .setConfigId(snowflake.nextId())
                .setConfigKey(request.getConfigKey())
                .setCategory(resolveConfigCategory(request.getConfigKey()))
                .setCreateTime(LocalDateTime.now());
        config.setConfigValue(request.getConfigValue())
                .setUpdatedBy(context.userId())
                .setUpdateTime(LocalDateTime.now());
        if (existingConfig != null) {
            repository.updateConfig(config);
        } else {
            repository.saveConfig(config);
        }
        saveLog("config", context, "update config " + request.getConfigKey(), String.valueOf(context.userId()));
        saveAudit(AuditTrail.builder()
                .actionType("config.update")
                .operatorId(context.userId())
                .targetType("config")
                .targetId(request.getConfigKey())
                .requestId(context.requestId())
                .detail(existingConfig == null
                        ? "created category=%s;value=%s".formatted(config.getCategory(), config.getConfigValue())
                        : "updated category=%s;value=%s->%s".formatted(
                        config.getCategory(), oldConfigValue, config.getConfigValue()))
                .occurredAt(LocalDateTime.now())
                .build());
        publishConfigUpdatedNotification(request.getConfigKey(), context, existingConfig == null);
        return ApiResponse.ok("ok").withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<?> queryAlarms(AlarmQueryRequest request, RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        List<AlarmSettingVO> records = repository.listAlarms().stream()
                .filter(item -> request.getAlarmType() == null || item.getAlarmType().equalsIgnoreCase(request.getAlarmType()))
                .sorted((left, right) -> right.getUpdateTime().compareTo(left.getUpdateTime()))
                .map(item -> AlarmSettingVO.builder()
                        .alarmId(item.getAlarmId())
                        .alarmType(item.getAlarmType())
                        .threshold(item.getThreshold())
                        .recipients(item.getRecipients())
                        .build())
                .toList();
        return ApiResponse.ok(page(records, request.getPageNum(), request.getPageSize()))
                .withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<?> updateAlarmSetting(AlarmSettingRequest request, RequestContext context) throws BusinessException {
        ensureSuperAdmin(context);
        SystemAlarmSettingEntity existingAlarm = repository.findAlarmByType(request.getAlarmType()).orElse(null);
        String oldThreshold = existingAlarm == null ? null : existingAlarm.getThreshold();
        List<String> oldRecipients = existingAlarm == null ? null : existingAlarm.getRecipients();
        SystemAlarmSettingEntity alarm = existingAlarm != null
                ? existingAlarm
                : new SystemAlarmSettingEntity()
                .setAlarmId(snowflake.nextId())
                .setAlarmType(request.getAlarmType())
                .setCreateTime(LocalDateTime.now());
        alarm.setThreshold(request.getThreshold())
                .setRecipients(request.getRecipients())
                .setUpdatedBy(context.userId())
                .setUpdateTime(LocalDateTime.now());
        if (existingAlarm != null) {
            repository.updateAlarm(alarm);
        } else {
            repository.saveAlarm(alarm);
        }
        saveLog("alarm", context, "update alarm " + request.getAlarmType(), null);
        saveAudit(AuditTrail.builder()
                .actionType("alarm.update")
                .operatorId(context.userId())
                .targetType("alarm")
                .targetId(request.getAlarmType())
                .requestId(context.requestId())
                .detail(existingAlarm == null
                        ? "created threshold=%s;recipients=%s".formatted(alarm.getThreshold(), alarm.getRecipients())
                        : "updated threshold=%s->%s;recipients=%s->%s".formatted(
                        oldThreshold, alarm.getThreshold(), oldRecipients, alarm.getRecipients()))
                .occurredAt(LocalDateTime.now())
                .build());
        return ApiResponse.ok("ok").withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<?> queryLogs(SystemLogQueryRequest request, RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        if (RoleMappings.isTeacher(context.roleId()) || RoleMappings.isStudent(context.roleId())) {
            throw new BusinessException(DomainErrorCode.SYSTEM_OPERATION_FORBIDDEN);
        }
        LocalDateTime start = parseTimeOrNull(request.getStartTime());
        LocalDateTime end = parseTimeOrNull(request.getEndTime());
        List<SystemLogVO> records = repository.listLogs().stream()
                .filter(item -> request.getLogType() == null || item.getLogType().equalsIgnoreCase(request.getLogType()))
                .filter(item -> request.getOperator() == null || containsIgnoreCase(item.getOperator(), request.getOperator()))
                .filter(item -> withinTimeRange(item.getCreateTime(), start, end))
                .sorted((left, right) -> right.getCreateTime().compareTo(left.getCreateTime()))
                .map(item -> SystemLogVO.builder()
                        .logId(item.getLogId())
                        .logType(item.getLogType())
                        .operatorId(item.getOperatorId())
                        .operator(item.getOperator())
                        .detail(item.getDetail())
                        .approverId(item.getApproverId())
                        .createTime(String.valueOf(item.getCreateTime()))
                        .build())
                .toList();
        return ApiResponse.ok(page(records, request.getPageNum(), request.getPageSize()))
                .withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<SystemLogExportView> exportLogs(
            String logType,
            String startTime,
            String endTime,
            String approverId,
            RequestContext context)
            throws BusinessException {
        ensureAuthenticated(context);
        if (!RoleMappings.isAuditor(context.roleId()) && !Integer.valueOf(1).equals(context.roleId())) {
            throw new BusinessException(DomainErrorCode.SYSTEM_LOG_EXPORT_FORBIDDEN);
        }
        if (RoleMappings.isAuditor(context.roleId()) && !StringUtils.hasText(approverId)) {
            throw new BusinessException(DomainErrorCode.LOG_EXPORT_APPROVAL_REQUIRED);
        }
        LocalDateTime start = parseTimeOrNull(startTime);
        LocalDateTime end = parseTimeOrNull(endTime);
        List<SystemLogEntity> logs = repository.listLogs().stream()
                .filter(item -> logType == null || item.getLogType().equalsIgnoreCase(logType))
                .filter(item -> approverId == null || approverId.equals(item.getApproverId()))
                .filter(item -> withinTimeRange(item.getCreateTime(), start, end))
                .sorted((left, right) -> right.getCreateTime().compareTo(left.getCreateTime()))
                .toList();
        saveAudit(AuditTrail.builder()
                .actionType("system.log.export")
                .operatorId(context.userId())
                .targetType("system_log")
                .targetId(logType == null ? "*" : logType)
                .requestId(context.requestId())
                .detail("count=" + logs.size())
                .occurredAt(LocalDateTime.now())
                .build());
        List<String> lines = new java.util.ArrayList<>();
        lines.add("logId,logType,operator,detail,approverId,createTime");
        logs.forEach(item -> lines.add("%s,%s,%s,%s,%s,%s".formatted(
                item.getLogId(),
                sanitize(item.getLogType()),
                sanitize(item.getOperator()),
                sanitize(item.getDetail()),
                sanitize(item.getApproverId()),
                item.getCreateTime())));
        LocalDateTime now = LocalDateTime.now();
        String fileKey = "system-log-export-" + snowflake.nextIdStr();
        String fileName = "system-log-export-" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".csv";
        resourceFileStore.writeCsv(fileKey, String.join(System.lineSeparator(), lines));
        return ApiResponse.ok(SystemLogExportView.builder()
                        .fileKey(fileKey)
                        .fileName(fileName)
                        .recordCount(logs.size())
                        .generatedAt(now)
                        .build())
                .withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<?> queryBackups(DataQueryRequest request, RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        if (!Integer.valueOf(1).equals(context.roleId())
                && !RoleMappings.isAuditor(context.roleId())
                && !RoleMappings.isOps(context.roleId())) {
            throw new BusinessException(DomainErrorCode.SYSTEM_BACKUP_QUERY_FORBIDDEN);
        }
        String backupType = normalizeBackupType(request.getBackupType());
        LocalDateTime start = parseTimeOrNull(request.getStartTime());
        LocalDateTime end = parseTimeOrNull(request.getEndTime());
        List<BackupRecordVO> records = repository.listBackups().stream()
                .filter(item -> backupType == null || item.getBackupType().equalsIgnoreCase(backupType))
                .filter(item -> request.getStatus() == null || item.getStatus().equalsIgnoreCase(request.getStatus()))
                .filter(item -> withinTimeRange(item.getCreateTime(), start, end))
                .sorted((left, right) -> right.getCreateTime().compareTo(left.getCreateTime()))
                .map(item -> BackupRecordVO.builder()
                        .backupId(item.getBackupId())
                        .backupType(item.getBackupType())
                        .status(item.getStatus())
                        .remark(item.getRemark())
                        .operatorId(item.getOperatorId())
                        .createTime(String.valueOf(item.getCreateTime()))
                        .updateTime(String.valueOf(item.getUpdateTime()))
                        .canRestore(canRestore(item))
                        .lifecycleStage(resolveBackupLifecycleStage(item.getStatus()))
                        .build())
                .toList();
        return ApiResponse.ok(page(records, request.getPageNum(), request.getPageSize()))
                .withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<?> backup(DataBackupRequest request, RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        if (!RoleMappings.isOps(context.roleId())) {
            throw new BusinessException(DomainErrorCode.SYSTEM_BACKUP_FORBIDDEN);
        }
        String backupType = normalizeBackupType(request.getBackupType());
        LocalDateTime now = LocalDateTime.now();
        SystemBackupRecordEntity backup = new SystemBackupRecordEntity()
                .setBackupId(snowflake.nextId())
                .setBackupType(backupType)
                .setStatus(BackupTaskStatus.BACKUP_PENDING.name())
                .setRemark(mergeRemark(request.getRemark(), "lifecycle=BACKUP_PENDING"))
                .setOperatorId(context.userId())
                .setCreateTime(now)
                .setUpdateTime(now);
        repository.saveBackup(backup);
        publishNotification(SystemNotificationEvent.builder()
                .eventType("backup.accepted")
                .targetType("backup")
                .targetId(String.valueOf(backup.getBackupId()))
                .operatorId(context.userId())
                .requestId(context.requestId())
                .occurredAt(now)
                .summary("备份任务已受理：backupId=%s,type=%s,status=%s".formatted(
                        backup.getBackupId(), backupType, backup.getStatus()))
                .build());
        taskExecutor.execute(() -> executeBackupTask(backup.getBackupId(), context, request.getRemark()));
        return ApiResponse.ok(BackupTaskAcceptedVO.builder()
                .backupId(backup.getBackupId())
                .status(backup.getStatus())
                .lifecycleStage(resolveBackupLifecycleStage(backup.getStatus()))
                .build()).withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<?> restore(DataRestoreRequest request, RequestContext context) throws BusinessException {
        ensureSuperAdmin(context);
        ensureRestoreFactors(request.getVerifyCode1(), request.getVerifyCode2());
        SystemBackupRecordEntity backup = repository.findBackupById(request.getBackupId())
                .orElseThrow(() -> new BusinessException(DomainErrorCode.BACKUP_RECORD_NOT_FOUND));
        if (!BackupTaskStatus.find(backup.getStatus()).map(BackupTaskStatus::isRestoreEnterable).orElse(false)) {
            throw new BusinessException(DomainErrorCode.SYSTEM_STATUS_CONFLICT);
        }
        String previousStatus = backup.getStatus();
        LocalDateTime restoreTime = LocalDateTime.now();
        backup.setStatus(BackupTaskStatus.RESTORE_PENDING.name())
                .setRemark(mergeRemark(backup.getRemark(),
                        "restoreRequestedBy=" + context.userId(),
                        "lifecycle=%s->%s".formatted(defaultDetail(previousStatus), BackupTaskStatus.RESTORE_PENDING.name())))
                .setUpdateTime(restoreTime);
        repository.updateBackup(backup);
        publishNotification(SystemNotificationEvent.builder()
                .eventType("restore.accepted")
                .targetType("backup")
                .targetId(String.valueOf(backup.getBackupId()))
                .operatorId(context.userId())
                .requestId(context.requestId())
                .occurredAt(restoreTime)
                .summary("恢复任务已受理：backupId=%s,status=%s".formatted(backup.getBackupId(), backup.getStatus()))
                .build());
        taskExecutor.execute(() -> executeRestoreTask(backup.getBackupId(), context));
        return ApiResponse.ok(BackupTaskAcceptedVO.builder()
                .backupId(backup.getBackupId())
                .status(backup.getStatus())
                .lifecycleStage(resolveBackupLifecycleStage(backup.getStatus()))
                .build()).withRequestId(context.requestId());
    }

    public void recoverInterruptedBackupTasks() {
        repository.listBackups().stream()
                .filter(item -> BackupTaskStatus.find(item.getStatus())
                        .map(BackupTaskStatus::isRecoverableRunningStatus)
                        .orElse(false))
                .forEach(this::markRecoveredTaskAsFailed);
    }

    private PermissionQueryVO buildTeacherPermissions() {
        return PermissionQueryVO.builder()
                .roleType("teacher")
                .menuList(List.of(
                        new MenuPermissionVO("class", "我的班级", "/teacher/class", "School"),
                        new MenuPermissionVO("question", "试题管理", "/teacher/question", "EditPen"),
                        new MenuPermissionVO("paper", "试卷管理", "/teacher/paper", "Document"),
                        new MenuPermissionVO("exam", "考试管理", "/teacher/exam", "Tickets"),
                        new MenuPermissionVO("score", "成绩管理", "/teacher/score", "DataAnalysis"),
                        new MenuPermissionVO("issue", "问题申报", "/teacher/issue", "Warning"),
                        new MenuPermissionVO("personal", "个人中心", "/personal", "User")))
                .routeList(List.of(
                        new RoutePermissionVO("/teacher", "TeacherHomePage"),
                        new RoutePermissionVO("/teacher/class", "TeacherClassPage"),
                        new RoutePermissionVO("/teacher/question", "TeacherQuestionPage"),
                        new RoutePermissionVO("/teacher/paper", "TeacherPaperPage"),
                        new RoutePermissionVO("/teacher/exam", "TeacherExamPage"),
                        new RoutePermissionVO("/teacher/score", "TeacherScorePage"),
                        new RoutePermissionVO("/teacher/issue", "TeacherIssuePage"),
                        new RoutePermissionVO("/personal", "PersonalCenterPage")))
                .buttonList(List.of(
                        "class.query",
                        "class.create",
                        "class.approveJoin",
                        "class.removeStudent",
                        "question.query",
                        "question.create",
                        "question.update",
                        "question.toggleStatus",
                        "question.import",
                        "question.export",
                        "paper.createManual",
                        "paper.createAuto",
                        "paper.publish",
                        "paper.terminate",
                        "paper.recycle",
                        "paper.export",
                        "exam.create",
                        "exam.updateParams",
                        "exam.distribute",
                        "exam.toggleStatus",
                        "exam.approveRetest",
                        "score.manualScore",
                        "score.publish",
                        "score.export",
                        "score.handleAppeal",
                        "issue.handle",
                        "issue.transfer",
                        "issue.close"))
                .build();
    }

    private PermissionQueryVO buildAdminPermissions() {
        return PermissionQueryVO.builder()
                .roleType("admin")
                .menuList(List.of(
                        new MenuPermissionVO("account", "账户管理", "/admin/account", "User"),
                        new MenuPermissionVO("class", "班级管理", "/admin/class", "School"),
                        new MenuPermissionVO("question-audit", "试题审核", "/admin/question-audit", "Checked"),
                        new MenuPermissionVO("system-config", "系统配置", "/admin/system-config", "Setting"),
                        new MenuPermissionVO("issue", "问题申报", "/admin/issue", "Warning"),
                        new MenuPermissionVO("personal", "个人中心", "/personal", "User")))
                .routeList(List.of(
                        new RoutePermissionVO("/admin", "AdminHomePage"),
                        new RoutePermissionVO("/admin/account", "AdminAccountPage"),
                        new RoutePermissionVO("/admin/class", "AdminClassPage"),
                        new RoutePermissionVO("/admin/question-audit", "AdminQuestionAuditPage"),
                        new RoutePermissionVO("/admin/system-config", "AdminSystemConfigPage"),
                        new RoutePermissionVO("/admin/issue", "AdminIssuePage"),
                        new RoutePermissionVO("/admin/log", "AdminLogPage"),
                        new RoutePermissionVO("/personal", "PersonalCenterPage")))
                .buttonList(List.of(
                        "account.query",
                        "account.create",
                        "account.audit",
                        "account.freeze",
                        "account.resetPassword",
                        "class.query",
                        "class.create",
                        "class.update",
                        "class.delete",
                        "class.import",
                        "question.query",
                        "question.audit",
                        "question.export",
                        "issue.handle",
                        "issue.transfer",
                        "issue.close"))
                .build();
    }

    private PermissionQueryVO buildSuperAdminPermissions() {
        return PermissionQueryVO.builder()
                .roleType("super_admin")
                .menuList(List.of(
                        new MenuPermissionVO("account", "账户管理", "/admin/account", "User"),
                        new MenuPermissionVO("class", "班级管理", "/admin/class", "School"),
                        new MenuPermissionVO("question-audit", "试题审核", "/admin/question-audit", "Checked"),
                        new MenuPermissionVO("system-config", "系统配置", "/admin/system-config", "Setting"),
                        new MenuPermissionVO("role", "角色权限管理", "/super-admin/role", "Lock"),
                        new MenuPermissionVO("log", "日志审计", "/super-admin/log", "Document"),
                        new MenuPermissionVO("data-security", "数据安全中心", "/super-admin/data-security", "Safe"),
                        new MenuPermissionVO("issue", "问题申报", "/admin/issue", "Warning"),
                        new MenuPermissionVO("personal", "个人中心", "/personal", "User")))
                .routeList(List.of(
                        new RoutePermissionVO("/super-admin", "SuperAdminHomePage"),
                        new RoutePermissionVO("/admin/account", "AdminAccountPage"),
                        new RoutePermissionVO("/admin/class", "AdminClassPage"),
                        new RoutePermissionVO("/admin/question-audit", "AdminQuestionAuditPage"),
                        new RoutePermissionVO("/admin/system-config", "AdminSystemConfigPage"),
                        new RoutePermissionVO("/admin/log", "AdminLogPage"),
                        new RoutePermissionVO("/super-admin/role", "SuperAdminRolePage"),
                        new RoutePermissionVO("/super-admin/log", "SuperAdminLogPage"),
                        new RoutePermissionVO("/super-admin/data-security", "SuperAdminDataSecurityPage"),
                        new RoutePermissionVO("/admin/issue", "AdminIssuePage"),
                        new RoutePermissionVO("/personal", "PersonalCenterPage")))
                .buttonList(List.of(
                        "account.query",
                        "account.create",
                        "account.audit",
                        "account.freeze",
                        "account.resetPassword",
                        "class.query",
                        "class.create",
                        "class.update",
                        "class.delete",
                        "class.import",
                        "question.query",
                        "question.audit",
                        "question.export",
                        "issue.handle",
                        "issue.transfer",
                        "issue.close",
                        "system.config.update",
                        "system.alarm.setting",
                        "system.log.export",
                        "system.role.save",
                        "system.permission.assign",
                        "system.data.backup",
                        "system.data.restore"))
                .build();
    }

    private PermissionQueryVO buildStudentPermissions() {
        return PermissionQueryVO.builder()
                .roleType("student")
                .menuList(List.of(
                        new MenuPermissionVO("class", "我的班级", "/student/class", "School"),
                        new MenuPermissionVO("exam", "我的考试", "/student/exam", "Tickets"),
                        new MenuPermissionVO("score", "成绩查询", "/student/score", "DataAnalysis"),
                        new MenuPermissionVO("issue", "问题申报", "/student/issue", "Warning"),
                        new MenuPermissionVO("personal", "个人中心", "/personal", "User")))
                .routeList(List.of(
                        new RoutePermissionVO("/student", "StudentHomePage"),
                        new RoutePermissionVO("/student/class", "StudentClassPage"),
                        new RoutePermissionVO("/student/exam", "StudentExamPage"),
                        new RoutePermissionVO("/student/exam/answer", "StudentExamAnswerPage"),
                        new RoutePermissionVO("/student/score", "StudentScorePage"),
                        new RoutePermissionVO("/student/issue", "StudentIssuePage"),
                        new RoutePermissionVO("/personal", "PersonalCenterPage")))
                .buttonList(List.of(
                        "class.applyJoin",
                        "class.quit",
                        "exam.enter",
                        "exam.saveProgress",
                        "exam.submit",
                        "score.applyRecheck",
                        "issue.create"))
                .build();
    }

    private PermissionQueryVO buildAuditorPermissions() {
        return PermissionQueryVO.builder()
                .roleType("auditor")
                .menuList(List.of(
                        new MenuPermissionVO("log", "日志审计", "/auditor/log", "Document"),
                        new MenuPermissionVO("score-audit", "成绩核查", "/auditor/score-audit", "DataAnalysis"),
                        new MenuPermissionVO("alarm", "异常行为监控", "/auditor/alarm", "Bell"),
                        new MenuPermissionVO("issue", "问题跟踪", "/auditor/issue", "Warning"),
                        new MenuPermissionVO("personal", "个人中心", "/personal", "User")))
                .routeList(List.of(
                        new RoutePermissionVO("/auditor", "AuditorHomePage"),
                        new RoutePermissionVO("/auditor/log", "AuditorLogPage"),
                        new RoutePermissionVO("/auditor/score-audit", "AuditorScorePage"),
                        new RoutePermissionVO("/auditor/alarm", "AuditorAlarmPage"),
                        new RoutePermissionVO("/auditor/issue", "AuditorIssuePage"),
                        new RoutePermissionVO("/personal", "PersonalCenterPage")))
                .buttonList(List.of(
                        "system.log.export"))
                .build();
    }

    private PermissionQueryVO buildOpsPermissions() {
        return PermissionQueryVO.builder()
                .roleType("ops")
                .menuList(List.of(
                        new MenuPermissionVO("alarm", "系统告警", "/ops/alarm", "Bell"),
                        new MenuPermissionVO("data-security", "数据安全中心", "/ops/data-security", "Safe"),
                        new MenuPermissionVO("issue", "系统类问题", "/ops/issue", "Warning"),
                        new MenuPermissionVO("log", "系统日志", "/ops/log", "Document"),
                        new MenuPermissionVO("personal", "个人中心", "/personal", "User")))
                .routeList(List.of(
                        new RoutePermissionVO("/ops", "OpsHomePage"),
                        new RoutePermissionVO("/ops/alarm", "OpsAlarmPage"),
                        new RoutePermissionVO("/ops/data-security", "OpsDataSecurityPage"),
                        new RoutePermissionVO("/ops/issue", "OpsIssuePage"),
                        new RoutePermissionVO("/ops/log", "OpsLogPage"),
                        new RoutePermissionVO("/personal", "PersonalCenterPage")))
                .buttonList(List.of(
                        "system.data.backup",
                        "issue.handle",
                        "issue.transfer",
                        "issue.close"))
                .build();
    }

    private void executeBackupTask(Long backupId, RequestContext context, String requestRemark) {
        SystemBackupRecordEntity backup = repository.findBackupById(backupId).orElse(null);
        if (backup == null) {
            return;
        }
        LocalDateTime runningTime = LocalDateTime.now();
        updateBackupStatus(backup, BackupTaskStatus.BACKUP_RUNNING, runningTime,
                "lifecycle=%s->%s".formatted(BackupTaskStatus.BACKUP_PENDING.name(), BackupTaskStatus.BACKUP_RUNNING.name()));
        try {
            maintenanceTaskRunner.executeBackup(backup, context);
            LocalDateTime finishTime = LocalDateTime.now();
            updateBackupStatus(backup, BackupTaskStatus.BACKUP_SUCCESS, finishTime,
                    "lifecycle=%s->%s".formatted(BackupTaskStatus.BACKUP_RUNNING.name(), BackupTaskStatus.BACKUP_SUCCESS.name()));
            saveLog("backup", context, "backup %s task %s completed".formatted(backup.getBackupType(), backupId), null);
            saveAudit(AuditTrail.builder()
                    .actionType("backup.execute")
                    .operatorId(context.userId())
                    .targetType("backup")
                    .targetId(String.valueOf(backupId))
                    .requestId(context.requestId())
                    .detail("type=%s;status=%s->%s;remark=%s".formatted(
                            backup.getBackupType(),
                            BackupTaskStatus.BACKUP_RUNNING.name(),
                            BackupTaskStatus.BACKUP_SUCCESS.name(),
                            defaultDetail(requestRemark)))
                    .occurredAt(finishTime)
                    .build());
            publishNotification(SystemNotificationEvent.builder()
                    .eventType("backup.succeeded")
                    .targetType("backup")
                    .targetId(String.valueOf(backupId))
                    .operatorId(context.userId())
                    .requestId(context.requestId())
                    .occurredAt(finishTime)
                    .summary("备份任务执行成功：backupId=%s,type=%s".formatted(backupId, backup.getBackupType()))
                    .build());
        } catch (RuntimeException exception) {
            markBackupExecutionFailed(backup, context, requestRemark, exception);
        }
    }

    private void executeRestoreTask(Long backupId, RequestContext context) {
        SystemBackupRecordEntity backup = repository.findBackupById(backupId).orElse(null);
        if (backup == null) {
            return;
        }
        LocalDateTime runningTime = LocalDateTime.now();
        updateBackupStatus(backup, BackupTaskStatus.RESTORE_RUNNING, runningTime,
                "lifecycle=%s->%s".formatted(BackupTaskStatus.RESTORE_PENDING.name(), BackupTaskStatus.RESTORE_RUNNING.name()));
        try {
            maintenanceTaskRunner.executeRestore(backup, context);
            LocalDateTime finishTime = LocalDateTime.now();
            updateBackupStatus(backup, BackupTaskStatus.RESTORE_SUCCESS, finishTime,
                    "restoredBy=" + context.userId(),
                    "restoredAt=" + finishTime.format(DATE_TIME_FORMATTER),
                    "lifecycle=%s->%s".formatted(BackupTaskStatus.RESTORE_RUNNING.name(), BackupTaskStatus.RESTORE_SUCCESS.name()));
            saveLog("restore", context, "restore backup %s completed".formatted(backupId), String.valueOf(context.userId()));
            saveAudit(AuditTrail.builder()
                    .actionType("backup.restore")
                    .operatorId(context.userId())
                    .targetType("backup")
                    .targetId(String.valueOf(backupId))
                    .requestId(context.requestId())
                    .detail("status=%s->%s;restoredBy=%s".formatted(
                            BackupTaskStatus.RESTORE_RUNNING.name(),
                            BackupTaskStatus.RESTORE_SUCCESS.name(),
                            context.userId()))
                    .occurredAt(finishTime)
                    .build());
            publishNotification(SystemNotificationEvent.builder()
                    .eventType("restore.succeeded")
                    .targetType("backup")
                    .targetId(String.valueOf(backupId))
                    .operatorId(context.userId())
                    .requestId(context.requestId())
                    .occurredAt(finishTime)
                    .summary("恢复任务执行成功：backupId=%s".formatted(backupId))
                    .build());
        } catch (RuntimeException exception) {
            markRestoreExecutionFailed(backup, context, exception);
        }
    }

    private void markBackupExecutionFailed(SystemBackupRecordEntity backup,
                                           RequestContext context,
                                           String requestRemark,
                                           RuntimeException exception) {
        LocalDateTime failedTime = LocalDateTime.now();
        updateBackupStatus(backup, BackupTaskStatus.BACKUP_FAILED, failedTime,
                "failedReason=" + sanitizeForRemark(exception.getMessage()),
                "lifecycle=%s->%s".formatted(BackupTaskStatus.BACKUP_RUNNING.name(), BackupTaskStatus.BACKUP_FAILED.name()));
        saveLog("backup", context, "backup %s task %s failed".formatted(backup.getBackupType(), backup.getBackupId()), null);
        saveAudit(AuditTrail.builder()
                .actionType("backup.execute")
                .operatorId(context.userId())
                .targetType("backup")
                .targetId(String.valueOf(backup.getBackupId()))
                .requestId(context.requestId())
                .detail("type=%s;status=%s->%s;remark=%s;error=%s".formatted(
                        backup.getBackupType(),
                        BackupTaskStatus.BACKUP_RUNNING.name(),
                        BackupTaskStatus.BACKUP_FAILED.name(),
                        defaultDetail(requestRemark),
                        sanitizeForRemark(exception.getMessage())))
                .occurredAt(failedTime)
                .build());
        publishNotification(SystemNotificationEvent.builder()
                .eventType("backup.failed")
                .targetType("backup")
                .targetId(String.valueOf(backup.getBackupId()))
                .operatorId(context.userId())
                .requestId(context.requestId())
                .occurredAt(failedTime)
                .summary("备份任务执行失败：backupId=%s,error=%s".formatted(
                        backup.getBackupId(), sanitizeForRemark(exception.getMessage())))
                .build());
    }

    private void markRestoreExecutionFailed(SystemBackupRecordEntity backup,
                                            RequestContext context,
                                            RuntimeException exception) {
        LocalDateTime failedTime = LocalDateTime.now();
        updateBackupStatus(backup, BackupTaskStatus.RESTORE_FAILED, failedTime,
                "restoreFailedReason=" + sanitizeForRemark(exception.getMessage()),
                "lifecycle=%s->%s".formatted(BackupTaskStatus.RESTORE_RUNNING.name(), BackupTaskStatus.RESTORE_FAILED.name()));
        saveLog("restore", context, "restore backup %s failed".formatted(backup.getBackupId()), String.valueOf(context.userId()));
        saveAudit(AuditTrail.builder()
                .actionType("backup.restore")
                .operatorId(context.userId())
                .targetType("backup")
                .targetId(String.valueOf(backup.getBackupId()))
                .requestId(context.requestId())
                .detail("status=%s->%s;error=%s".formatted(
                        BackupTaskStatus.RESTORE_RUNNING.name(),
                        BackupTaskStatus.RESTORE_FAILED.name(),
                        sanitizeForRemark(exception.getMessage())))
                .occurredAt(failedTime)
                .build());
        publishNotification(SystemNotificationEvent.builder()
                .eventType("restore.failed")
                .targetType("backup")
                .targetId(String.valueOf(backup.getBackupId()))
                .operatorId(context.userId())
                .requestId(context.requestId())
                .occurredAt(failedTime)
                .summary("恢复任务执行失败：backupId=%s,error=%s".formatted(
                        backup.getBackupId(), sanitizeForRemark(exception.getMessage())))
                .build());
    }

    private void markRecoveredTaskAsFailed(SystemBackupRecordEntity backup) {
        BackupTaskStatus currentStatus = BackupTaskStatus.find(backup.getStatus()).orElse(null);
        if (currentStatus == null) {
            return;
        }
        BackupTaskStatus failedStatus = currentStatus.toRecoveredFailureStatus();
        LocalDateTime recoveredAt = LocalDateTime.now();
        updateBackupStatus(backup, failedStatus, recoveredAt,
                "recoveredAfterRestart=true",
                "lifecycle=%s->%s".formatted(currentStatus.name(), failedStatus.name()));
        publishNotification(SystemNotificationEvent.builder()
                .eventType(failedStatus == BackupTaskStatus.BACKUP_FAILED ? "backup.failed" : "restore.failed")
                .targetType("backup")
                .targetId(String.valueOf(backup.getBackupId()))
                .operatorId(backup.getOperatorId())
                .requestId(null)
                .occurredAt(recoveredAt)
                .summary("任务因服务重启回收为失败：backupId=%s,status=%s".formatted(
                        backup.getBackupId(), failedStatus.name()))
                .build());
    }

    private void updateBackupStatus(SystemBackupRecordEntity backup,
                                    BackupTaskStatus nextStatus,
                                    LocalDateTime updateTime,
                                    String... extraRemarks) {
        backup.setStatus(nextStatus.name())
                .setRemark(mergeRemark(backup.getRemark(), extraRemarks))
                .setUpdateTime(updateTime);
        repository.updateBackup(backup);
    }

    private void publishConfigUpdatedNotification(String configKey, RequestContext context, boolean created) {
        publishNotification(SystemNotificationEvent.builder()
                .eventType("config.updated")
                .targetType("config")
                .targetId(configKey)
                .operatorId(context.userId())
                .requestId(context.requestId())
                .occurredAt(LocalDateTime.now())
                .summary(buildConfigNotificationSummary(configKey, created))
                .build());
    }

    private String buildConfigNotificationSummary(String configKey, boolean created) {
        if (isSensitiveConfigKey(configKey)) {
            return "%s 配置项 %s 已变更，值已脱敏".formatted(created ? "新建" : "更新", configKey);
        }
        return "%s 配置项 %s 已更新".formatted(created ? "新建" : "更新", configKey);
    }

    private boolean isSensitiveConfigKey(String configKey) {
        if (!StringUtils.hasText(configKey)) {
            return false;
        }
        String normalized = configKey.toLowerCase(Locale.ROOT);
        return SENSITIVE_CONFIG_KEYWORDS.stream().anyMatch(normalized::contains);
    }

    private void publishNotification(SystemNotificationEvent event) {
        notificationPublisher.publish(event);
    }

    private void ensureAuthenticated(RequestContext context) throws BusinessException {
        if (context.userId() == null || context.roleId() == null) {
            throw new BusinessException(DomainErrorCode.AUTHENTICATION_REQUIRED);
        }
    }

    private void ensureSuperAdmin(RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        if (!Integer.valueOf(1).equals(context.roleId())) {
            throw new BusinessException(DomainErrorCode.SUPER_ADMIN_REQUIRED);
        }
    }

    private void ensureSecondFactor(String verifyCode, String expectedCode) throws BusinessException {
        if (!expectedCode.equals(verifyCode)) {
            throw new BusinessException(DomainErrorCode.SECOND_FACTOR_VALIDATION_FAILED);
        }
    }

    private void ensureRestoreFactors(String verifyCode1, String verifyCode2) throws BusinessException {
        if (!RESTORE_CONFIRM_CODE.equals(verifyCode1) || !RESTORE_CONFIRM_CODE.equals(verifyCode2)) {
            throw new BusinessException(DomainErrorCode.SECOND_FACTOR_VALIDATION_FAILED);
        }
    }

    private String resolveConfigCategory(String configKey) {
        if (configKey == null || !configKey.contains(".")) {
            return "default";
        }
        return configKey.substring(0, configKey.indexOf('.')).toLowerCase(Locale.ROOT);
    }

    private String normalizeBackupType(String backupType) throws BusinessException {
        if (!StringUtils.hasText(backupType)) {
            return null;
        }
        String normalized = backupType.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_BACKUP_TYPES.contains(normalized)) {
            throw new BusinessException(DomainErrorCode.SYSTEM_BACKUP_TYPE_INVALID);
        }
        return normalized;
    }

    private void saveLog(String logType, RequestContext context, String detail, String approverId) {
        repository.saveLog(new SystemLogEntity()
                .setLogId(snowflake.nextId())
                .setLogType(logType)
                .setOperatorId(context.userId())
                .setOperator(resolveOperator(context))
                .setDetail(detail)
                .setApproverId(approverId)
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now()));
    }

    private void saveAudit(AuditTrail trail) {
        repository.saveAudit(new SystemAuditRecordEntity()
                .setAuditId(snowflake.nextId())
                .setActionType(trail.getActionType())
                .setOperatorId(trail.getOperatorId())
                .setTargetType(trail.getTargetType())
                .setTargetId(trail.getTargetId())
                .setRequestId(trail.getRequestId())
                .setDetail(trail.getDetail())
                .setCreateTime(trail.getOccurredAt())
                .setUpdateTime(trail.getOccurredAt()));
    }

    private boolean canRestore(SystemBackupRecordEntity backup) {
        return BackupTaskStatus.canRestore(backup.getStatus());
    }

    private String resolveBackupLifecycleStage(String status) {
        return BackupTaskStatus.resolveLifecycleStage(status);
    }

    private String mergeRemark(String baseRemark, String... extras) {
        List<String> segments = new ArrayList<>();
        if (StringUtils.hasText(baseRemark)) {
            segments.add(baseRemark.trim());
        }
        for (String extra : extras) {
            if (StringUtils.hasText(extra)) {
                segments.add(extra.trim());
            }
        }
        return String.join(" | ", segments);
    }

    private String defaultDetail(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

    private String sanitizeForRemark(String value) {
        if (!StringUtils.hasText(value)) {
            return "-";
        }
        return value.replace(System.lineSeparator(), " ").replace("|", "/").trim();
    }

    private LocalDateTime parseTimeOrNull(String value) throws BusinessException {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(DomainErrorCode.DATE_TIME_FORMAT_INVALID);
        }
    }

    private boolean withinTimeRange(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        if (value == null) {
            return false;
        }
        if (start != null && value.isBefore(start)) {
            return false;
        }
        return end == null || !value.isAfter(end);
    }

    private String resolveOperator(RequestContext context) {
        return switch (context.roleId()) {
            case 1 -> "super_admin";
            case 2 -> "admin";
            case 3 -> "teacher";
            case 4 -> "student";
            case 5 -> "auditor";
            case 6 -> "ops";
            default -> "unknown";
        };
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private <T> PageResult<T> page(List<T> source, int pageNum, int pageSize) {
        int fromIndex = Math.min((pageNum - 1) * pageSize, source.size());
        int toIndex = Math.min(fromIndex + pageSize, source.size());
        return new PageResult<>(source.subList(fromIndex, toIndex), source.size(), pageNum, pageSize);
    }
}
