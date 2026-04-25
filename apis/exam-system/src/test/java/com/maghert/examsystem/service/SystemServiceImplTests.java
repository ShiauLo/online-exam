package com.maghert.examsystem.service;

import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.web.PageResult;
import com.maghert.examsystem.context.RequestContext;
import com.maghert.examsystem.entity.SystemAlarmSettingEntity;
import com.maghert.examsystem.entity.SystemAuditRecordEntity;
import com.maghert.examsystem.entity.SystemBackupRecordEntity;
import com.maghert.examsystem.entity.SystemConfigEntity;
import com.maghert.examsystem.entity.SystemLogEntity;
import com.maghert.examsystem.entity.SystemNotificationRecordEntity;
import com.maghert.examsystem.entity.SystemPermissionAssignmentEntity;
import com.maghert.examsystem.entity.SystemRoleEntity;
import com.maghert.examsystem.model.dto.DataBackupRequest;
import com.maghert.examsystem.model.dto.DataQueryRequest;
import com.maghert.examsystem.model.dto.DataRestoreRequest;
import com.maghert.examsystem.model.dto.PermissionAssignRequest;
import com.maghert.examsystem.model.dto.PermissionQueryRequest;
import com.maghert.examsystem.model.dto.SystemConfigUpdateRequest;
import com.maghert.examsystem.model.dto.SystemLogQueryRequest;
import com.maghert.examsystem.model.enums.BackupTaskStatus;
import com.maghert.examsystem.model.vo.BackupRecordVO;
import com.maghert.examsystem.model.vo.BackupTaskAcceptedVO;
import com.maghert.examsystem.model.vo.PermissionQueryVO;
import com.maghert.examsystem.model.vo.SystemLogExportView;
import com.maghert.examsystem.model.vo.SystemLogVO;
import com.maghert.examsystem.notification.OutboxSystemNotificationPublisher;
import com.maghert.examsystem.notification.SystemNotificationPublisher;
import com.maghert.examsystem.repository.SystemDomainRepository;
import com.maghert.examsystem.service.impl.SystemServiceImpl;
import com.maghert.examsystem.task.SystemMaintenanceTaskRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemServiceImplTests {

    private InMemorySystemDomainRepository repository;
    private ManualTaskExecutor taskExecutor;
    private ConfigurableMaintenanceTaskRunner taskRunner;
    private SystemServiceImpl service;
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        repository = new InMemorySystemDomainRepository();
        taskExecutor = new ManualTaskExecutor();
        taskRunner = new ConfigurableMaintenanceTaskRunner();
        tempDir = Files.createDirectories(Path.of("target", "system-service-tests", Long.toString(System.nanoTime())));
        SystemNotificationPublisher notificationPublisher =
                new OutboxSystemNotificationPublisher(repository, new Snowflake(2, 1), new ObjectMapper());
        service = new SystemServiceImpl(
                new Snowflake(1, 1),
                repository,
                taskExecutor,
                notificationPublisher,
                taskRunner,
                tempDir.toString());
    }

    @Test
    void normalAdminShouldNotAssignSuperAdminRole() {
        PermissionAssignRequest request = new PermissionAssignRequest();
        request.setAccountId(1001L);
        request.setRoleId(1);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.assignPermission(request, new RequestContext(2001L, 2, "req-1")));
        assertEquals(403, exception.getCode());
    }

    @Test
    void auditorShouldBeReadOnlyForLogs() {
        DataBackupRequest request = new DataBackupRequest();
        request.setBackupType("full");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.backup(request, new RequestContext(5001L, 5, "req-auditor")));
        assertEquals(403, exception.getCode());
    }

    @Test
    void configUpdateShouldRequireSecondFactor() {
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigKey("security.password.policy");
        request.setConfigValue("strong");
        request.setVerifyCode("WRONG");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateConfig(request, new RequestContext(1L, 1, "req-super")));
        assertEquals(403, exception.getCode());
    }

    @Test
    void exportLogsShouldRequireAuditorOrSuperAdmin() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.exportLogs(null, null, null, null, new RequestContext(3001L, 3, "req-teacher")));
        assertEquals(403, exception.getCode());
    }

    @Test
    void auditorExportShouldRequireApproverId() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.exportLogs(null, null, null, null, new RequestContext(5001L, 5, "req-auditor")));
        assertEquals(403, exception.getCode());
    }

    @Test
    void queryLogsShouldRespectTimeRangeFilter() throws Exception {
        repository.logs.add(new SystemLogEntity()
                .setLogId(1L)
                .setLogType("config")
                .setOperator("super_admin")
                .setCreateTime(LocalDateTime.of(2026, 4, 2, 8, 0, 0)));
        repository.logs.add(new SystemLogEntity()
                .setLogId(2L)
                .setLogType("config")
                .setOperator("super_admin")
                .setCreateTime(LocalDateTime.of(2026, 4, 3, 8, 0, 0)));

        SystemLogQueryRequest request = new SystemLogQueryRequest();
        request.setStartTime("2026-04-03 00:00:00");
        request.setEndTime("2026-04-03 23:59:59");

        @SuppressWarnings("unchecked")
        PageResult<SystemLogVO> page = (PageResult<SystemLogVO>) service.queryLogs(
                request,
                new RequestContext(1L, 1, "req-super")).getData();

        assertEquals(1, page.getRecords().size());
        assertEquals(2L, page.getRecords().get(0).getLogId());
    }

    @Test
    void queryBackupsShouldRespectTimeRangeFilter() throws Exception {
        repository.backups.add(new SystemBackupRecordEntity()
                .setBackupId(1L)
                .setBackupType("full")
                .setStatus(BackupTaskStatus.BACKUP_SUCCESS.name())
                .setCreateTime(LocalDateTime.of(2026, 4, 2, 8, 0, 0))
                .setUpdateTime(LocalDateTime.of(2026, 4, 2, 8, 10, 0)));
        repository.backups.add(new SystemBackupRecordEntity()
                .setBackupId(2L)
                .setBackupType("full")
                .setStatus(BackupTaskStatus.BACKUP_SUCCESS.name())
                .setCreateTime(LocalDateTime.of(2026, 4, 3, 8, 0, 0))
                .setUpdateTime(LocalDateTime.of(2026, 4, 3, 8, 10, 0)));

        DataQueryRequest request = new DataQueryRequest();
        request.setStartTime("2026-04-03 00:00:00");
        request.setEndTime("2026-04-03 23:59:59");

        @SuppressWarnings("unchecked")
        PageResult<BackupRecordVO> page =
                (PageResult<BackupRecordVO>) service.queryBackups(
                        request,
                        new RequestContext(1L, 1, "req-super")).getData();

        assertEquals(1, page.getRecords().size());
        assertEquals(2L, page.getRecords().get(0).getBackupId());
        assertTrue(Boolean.TRUE.equals(page.getRecords().get(0).getCanRestore()));
        assertEquals("available", page.getRecords().get(0).getLifecycleStage());
    }

    @Test
    void teacherShouldNotQueryBackups() {
        DataQueryRequest request = new DataQueryRequest();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.queryBackups(request, new RequestContext(3001L, 3, "req-teacher")));
        assertEquals(403, exception.getCode());
    }

    @Test
    void auditorExportWithApproverShouldWriteAuditTrail() throws Exception {
        repository.logs.add(new SystemLogEntity()
                .setLogId(1L)
                .setLogType("config")
                .setOperator("super_admin")
                .setApproverId("1")
                .setCreateTime(LocalDateTime.of(2026, 4, 3, 8, 0, 0)));

        SystemLogExportView exportView = service.exportLogs(
                "config",
                null,
                null,
                "1",
                new RequestContext(5001L, 5, "req-auditor")).getData();
        String csv = Files.readString(tempDir.resolve("exports").resolve(exportView.getFileKey() + ".csv"));

        assertTrue(csv.contains("config"));
        assertEquals(1, repository.audits.size());
        assertEquals("system.log.export", repository.audits.get(0).getActionType());
        assertEquals(1, exportView.getRecordCount());
    }

    @Test
    void configAuditShouldIncludeBeforeAndAfterValues() throws Exception {
        SystemConfigUpdateRequest createRequest = new SystemConfigUpdateRequest();
        createRequest.setConfigKey("security.password.policy");
        createRequest.setConfigValue("strong");
        createRequest.setVerifyCode("CONFIRM_SYSTEM_CONFIG");
        service.updateConfig(createRequest, new RequestContext(1L, 1, "req-1"));

        SystemConfigUpdateRequest updateRequest = new SystemConfigUpdateRequest();
        updateRequest.setConfigKey("security.password.policy");
        updateRequest.setConfigValue("very-strong");
        updateRequest.setVerifyCode("CONFIRM_SYSTEM_CONFIG");
        service.updateConfig(updateRequest, new RequestContext(1L, 1, "req-2"));

        assertTrue(repository.audits.stream()
                .anyMatch(item -> "config.update".equals(item.getActionType())
                        && item.getDetail().contains("strong")
                        && item.getDetail().contains("very-strong")));
    }

    @Test
    void configUpdateShouldWriteMaskedNotification() throws Exception {
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigKey("security.password.policy");
        request.setConfigValue("strong");
        request.setVerifyCode("CONFIRM_SYSTEM_CONFIG");

        service.updateConfig(request, new RequestContext(1L, 1, "req-1"));

        assertEquals(1, repository.notifications.size());
        SystemNotificationRecordEntity notification = repository.notifications.get(0);
        assertEquals("config.updated", notification.getEventType());
        assertTrue(notification.getPayload().contains("security.password.policy"));
        assertFalse(notification.getPayload().contains("strong"));
        assertTrue(notification.getPayload().contains("已脱敏"));
    }

    @Test
    void backupShouldReturnAcceptedThenBecomeSuccessAfterAsyncExecution() throws Exception {
        DataBackupRequest request = new DataBackupRequest();
        request.setBackupType("full");
        request.setRemark("nightly");

        BackupTaskAcceptedVO accepted = (BackupTaskAcceptedVO) service.backup(
                request,
                new RequestContext(6001L, 6, "req-ops")).getData();

        assertEquals(BackupTaskStatus.BACKUP_PENDING.name(), accepted.getStatus());
        assertEquals("accepted", accepted.getLifecycleStage());
        assertEquals(1, taskExecutor.size());
        assertEquals(1, repository.notifications.size());
        assertEquals("backup.accepted", repository.notifications.get(0).getEventType());

        taskExecutor.runAll();

        SystemBackupRecordEntity backup = repository.findBackupById(accepted.getBackupId()).orElseThrow();
        assertEquals(BackupTaskStatus.BACKUP_SUCCESS.name(), backup.getStatus());
        assertTrue(backup.getRemark().contains("BACKUP_RUNNING->BACKUP_SUCCESS"));
        assertTrue(repository.notifications.stream().anyMatch(item -> "backup.succeeded".equals(item.getEventType())));
    }

    @Test
    void backupFailureShouldBeMarkedAsFailedAndPublishNotification() throws Exception {
        taskRunner.failNextBackup("disk full");
        DataBackupRequest request = new DataBackupRequest();
        request.setBackupType("full");

        BackupTaskAcceptedVO accepted = (BackupTaskAcceptedVO) service.backup(
                request,
                new RequestContext(6001L, 6, "req-ops")).getData();
        taskExecutor.runAll();

        SystemBackupRecordEntity backup = repository.findBackupById(accepted.getBackupId()).orElseThrow();
        assertEquals(BackupTaskStatus.BACKUP_FAILED.name(), backup.getStatus());
        assertTrue(backup.getRemark().contains("failedReason=disk full"));
        assertTrue(repository.notifications.stream().anyMatch(item -> "backup.failed".equals(item.getEventType())));
    }

    @Test
    void restoreShouldReturnAcceptedThenBecomeSuccessAfterAsyncExecution() throws Exception {
        repository.saveBackup(new SystemBackupRecordEntity()
                .setBackupId(1L)
                .setBackupType("full")
                .setStatus(BackupTaskStatus.BACKUP_SUCCESS.name())
                .setRemark("seed")
                .setOperatorId(6001L)
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now()));

        DataRestoreRequest request = new DataRestoreRequest();
        request.setBackupId(1L);
        request.setVerifyCode1("CONFIRM_SYSTEM_RESTORE");
        request.setVerifyCode2("CONFIRM_SYSTEM_RESTORE");

        BackupTaskAcceptedVO accepted = (BackupTaskAcceptedVO) service.restore(
                request,
                new RequestContext(1L, 1, "req-super")).getData();

        assertEquals(BackupTaskStatus.RESTORE_PENDING.name(), accepted.getStatus());
        assertEquals("accepted", accepted.getLifecycleStage());

        taskExecutor.runAll();

        SystemBackupRecordEntity backup = repository.findBackupById(1L).orElseThrow();
        assertEquals(BackupTaskStatus.RESTORE_SUCCESS.name(), backup.getStatus());
        assertTrue(backup.getRemark().contains("restoredBy=1"));
        assertTrue(repository.notifications.stream().anyMatch(item -> "restore.succeeded".equals(item.getEventType())));
    }

    @Test
    void restoreFailureShouldRemainRetryable() throws Exception {
        repository.saveBackup(new SystemBackupRecordEntity()
                .setBackupId(1L)
                .setBackupType("full")
                .setStatus(BackupTaskStatus.BACKUP_SUCCESS.name())
                .setOperatorId(6001L)
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now()));
        taskRunner.failNextRestore("restore crashed");

        DataRestoreRequest request = new DataRestoreRequest();
        request.setBackupId(1L);
        request.setVerifyCode1("CONFIRM_SYSTEM_RESTORE");
        request.setVerifyCode2("CONFIRM_SYSTEM_RESTORE");

        service.restore(request, new RequestContext(1L, 1, "req-super"));
        taskExecutor.runAll();

        SystemBackupRecordEntity backup = repository.findBackupById(1L).orElseThrow();
        assertEquals(BackupTaskStatus.RESTORE_FAILED.name(), backup.getStatus());

        DataQueryRequest queryRequest = new DataQueryRequest();
        @SuppressWarnings("unchecked")
        PageResult<BackupRecordVO> page = (PageResult<BackupRecordVO>) service.queryBackups(
                queryRequest,
                new RequestContext(1L, 1, "req-super")).getData();
        BackupRecordVO record = page.getRecords().get(0);
        assertTrue(Boolean.TRUE.equals(record.getCanRestore()));
        assertEquals("failed", record.getLifecycleStage());
    }

    @Test
    void restoreShouldRejectUnsupportedState() throws Exception {
        repository.saveBackup(new SystemBackupRecordEntity()
                .setBackupId(1L)
                .setBackupType("full")
                .setStatus(BackupTaskStatus.RESTORE_SUCCESS.name()));

        DataRestoreRequest request = new DataRestoreRequest();
        request.setBackupId(1L);
        request.setVerifyCode1("CONFIRM_SYSTEM_RESTORE");
        request.setVerifyCode2("CONFIRM_SYSTEM_RESTORE");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.restore(request, new RequestContext(1L, 1, "req-super")));
        assertEquals(409, exception.getCode());
    }

    @Test
    void recoverInterruptedBackupTasksShouldConvertRunningStatesToFailed() {
        repository.saveBackup(new SystemBackupRecordEntity().setBackupId(1L).setBackupType("full")
                .setStatus(BackupTaskStatus.BACKUP_PENDING.name()).setOperatorId(1L));
        repository.saveBackup(new SystemBackupRecordEntity().setBackupId(2L).setBackupType("full")
                .setStatus(BackupTaskStatus.BACKUP_RUNNING.name()).setOperatorId(1L));
        repository.saveBackup(new SystemBackupRecordEntity().setBackupId(3L).setBackupType("full")
                .setStatus(BackupTaskStatus.RESTORE_PENDING.name()).setOperatorId(1L));
        repository.saveBackup(new SystemBackupRecordEntity().setBackupId(4L).setBackupType("full")
                .setStatus(BackupTaskStatus.RESTORE_RUNNING.name()).setOperatorId(1L));
        repository.saveBackup(new SystemBackupRecordEntity().setBackupId(5L).setBackupType("full")
                .setStatus(BackupTaskStatus.BACKUP_SUCCESS.name()).setOperatorId(1L));

        service.recoverInterruptedBackupTasks();

        assertEquals(BackupTaskStatus.BACKUP_FAILED.name(), repository.findBackupById(1L).orElseThrow().getStatus());
        assertEquals(BackupTaskStatus.BACKUP_FAILED.name(), repository.findBackupById(2L).orElseThrow().getStatus());
        assertEquals(BackupTaskStatus.RESTORE_FAILED.name(), repository.findBackupById(3L).orElseThrow().getStatus());
        assertEquals(BackupTaskStatus.RESTORE_FAILED.name(), repository.findBackupById(4L).orElseThrow().getStatus());
        assertEquals(BackupTaskStatus.BACKUP_SUCCESS.name(), repository.findBackupById(5L).orElseThrow().getStatus());
        assertTrue(repository.findBackupById(1L).orElseThrow().getRemark().contains("recoveredAfterRestart=true"));
        assertEquals(4, repository.notifications.size());
    }

    @Test
    void backupShouldRejectUnsupportedType() {
        DataBackupRequest backupRequest = new DataBackupRequest();
        backupRequest.setBackupType("snapshot");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.backup(backupRequest, new RequestContext(6001L, 6, "req-ops")));
        assertEquals(400, exception.getCode());
    }

    @Test
    void queryBackupsShouldRejectUnsupportedType() {
        DataQueryRequest request = new DataQueryRequest();
        request.setBackupType("snapshot");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.queryBackups(request, new RequestContext(1L, 1, "req-super")));
        assertEquals(400, exception.getCode());
    }

    @Test
    void permissionTemplateShouldKeepQuestionMenuForTeacher() throws Exception {
        PermissionQueryRequest request = new PermissionQueryRequest();
        PermissionQueryVO payload = (PermissionQueryVO) service.queryPermissions(
                request,
                new RequestContext(3001L, 3, "req-teacher")).getData();

        assertEquals("teacher", payload.getRoleType());
        assertTrue(payload.getRouteList().stream().anyMatch(item -> "/teacher/question".equals(item.getPath())));
        assertTrue(payload.getButtonList().contains("question.query"));
    }

    @Test
    void studentPermissionTemplateShouldContainLandingAnswerRouteAndExamButtons() throws Exception {
        PermissionQueryRequest request = new PermissionQueryRequest();
        PermissionQueryVO payload = (PermissionQueryVO) service.queryPermissions(
                request,
                new RequestContext(4001L, 4, "req-student")).getData();

        assertEquals("student", payload.getRoleType());
        assertTrue(payload.getRouteList().stream().anyMatch(item -> "/student".equals(item.getPath())));
        assertTrue(payload.getRouteList().stream().anyMatch(item -> "/student/exam/answer".equals(item.getPath())));
        assertTrue(payload.getRouteList().stream().anyMatch(item -> "/personal".equals(item.getPath())));
        assertTrue(payload.getButtonList().contains("exam.enter"));
        assertTrue(payload.getButtonList().contains("exam.saveProgress"));
        assertTrue(payload.getButtonList().contains("exam.submit"));
    }

    private static final class ManualTaskExecutor implements TaskExecutor {

        private final List<Runnable> tasks = new ArrayList<>();

        @Override
        public void execute(Runnable task) {
            tasks.add(task);
        }

        private void runAll() {
            List<Runnable> pending = List.copyOf(tasks);
            tasks.clear();
            pending.forEach(Runnable::run);
        }

        private int size() {
            return tasks.size();
        }
    }

    private static final class ConfigurableMaintenanceTaskRunner implements SystemMaintenanceTaskRunner {

        private String nextBackupFailure;
        private String nextRestoreFailure;

        private void failNextBackup(String message) {
            this.nextBackupFailure = message;
        }

        private void failNextRestore(String message) {
            this.nextRestoreFailure = message;
        }

        @Override
        public void executeBackup(SystemBackupRecordEntity backupRecord, RequestContext context) {
            if (nextBackupFailure != null) {
                String message = nextBackupFailure;
                nextBackupFailure = null;
                throw new IllegalStateException(message);
            }
        }

        @Override
        public void executeRestore(SystemBackupRecordEntity backupRecord, RequestContext context) {
            if (nextRestoreFailure != null) {
                String message = nextRestoreFailure;
                nextRestoreFailure = null;
                throw new IllegalStateException(message);
            }
        }
    }

    private static final class InMemorySystemDomainRepository implements SystemDomainRepository {

        private final List<SystemRoleEntity> roles = new ArrayList<>();
        private final List<SystemPermissionAssignmentEntity> assignments = new ArrayList<>();
        private final List<SystemConfigEntity> configs = new ArrayList<>();
        private final List<SystemAlarmSettingEntity> alarms = new ArrayList<>();
        private final List<SystemLogEntity> logs = new ArrayList<>();
        private final List<SystemBackupRecordEntity> backups = new ArrayList<>();
        private final List<SystemAuditRecordEntity> audits = new ArrayList<>();
        private final List<SystemNotificationRecordEntity> notifications = new ArrayList<>();

        @Override
        public SystemRoleEntity saveRole(SystemRoleEntity role) {
            roles.removeIf(item -> item.getRoleId().equals(role.getRoleId()));
            roles.add(role);
            return role;
        }

        @Override
        public void updateRole(SystemRoleEntity role) {
            saveRole(role);
        }

        @Override
        public Optional<SystemRoleEntity> findRoleById(Long roleId) {
            return roles.stream().filter(item -> roleId.equals(item.getRoleId())).findFirst();
        }

        @Override
        public List<SystemRoleEntity> listRoles() {
            return List.copyOf(roles);
        }

        @Override
        public SystemPermissionAssignmentEntity saveAssignment(SystemPermissionAssignmentEntity assignment) {
            assignments.removeIf(item -> item.getAccountId().equals(assignment.getAccountId()));
            assignments.add(assignment);
            return assignment;
        }

        @Override
        public void updateAssignment(SystemPermissionAssignmentEntity assignment) {
            saveAssignment(assignment);
        }

        @Override
        public Optional<SystemPermissionAssignmentEntity> findAssignmentByAccountId(Long accountId) {
            return assignments.stream().filter(item -> accountId.equals(item.getAccountId())).findFirst();
        }

        @Override
        public List<SystemPermissionAssignmentEntity> listAssignments() {
            return List.copyOf(assignments);
        }

        @Override
        public SystemConfigEntity saveConfig(SystemConfigEntity config) {
            configs.removeIf(item -> item.getConfigKey().equals(config.getConfigKey()));
            configs.add(config);
            return config;
        }

        @Override
        public void updateConfig(SystemConfigEntity config) {
            saveConfig(config);
        }

        @Override
        public Optional<SystemConfigEntity> findConfigByKey(String configKey) {
            return configs.stream().filter(item -> configKey.equals(item.getConfigKey())).findFirst();
        }

        @Override
        public List<SystemConfigEntity> listConfigs() {
            return List.copyOf(configs);
        }

        @Override
        public SystemAlarmSettingEntity saveAlarm(SystemAlarmSettingEntity alarmSetting) {
            alarms.removeIf(item -> item.getAlarmType().equals(alarmSetting.getAlarmType()));
            alarms.add(alarmSetting);
            return alarmSetting;
        }

        @Override
        public void updateAlarm(SystemAlarmSettingEntity alarmSetting) {
            saveAlarm(alarmSetting);
        }

        @Override
        public Optional<SystemAlarmSettingEntity> findAlarmByType(String alarmType) {
            return alarms.stream().filter(item -> alarmType.equals(item.getAlarmType())).findFirst();
        }

        @Override
        public List<SystemAlarmSettingEntity> listAlarms() {
            return List.copyOf(alarms);
        }

        @Override
        public SystemLogEntity saveLog(SystemLogEntity log) {
            logs.add(log);
            return log;
        }

        @Override
        public List<SystemLogEntity> listLogs() {
            return List.copyOf(logs);
        }

        @Override
        public SystemBackupRecordEntity saveBackup(SystemBackupRecordEntity backupRecord) {
            backups.removeIf(item -> item.getBackupId().equals(backupRecord.getBackupId()));
            backups.add(backupRecord);
            return backupRecord;
        }

        @Override
        public void updateBackup(SystemBackupRecordEntity backupRecord) {
            saveBackup(backupRecord);
        }

        @Override
        public Optional<SystemBackupRecordEntity> findBackupById(Long backupId) {
            return backups.stream().filter(item -> backupId.equals(item.getBackupId())).findFirst();
        }

        @Override
        public List<SystemBackupRecordEntity> listBackups() {
            return List.copyOf(backups);
        }

        @Override
        public void saveAudit(SystemAuditRecordEntity auditRecord) {
            audits.add(auditRecord);
        }

        @Override
        public SystemNotificationRecordEntity saveNotification(SystemNotificationRecordEntity notificationRecord) {
            notifications.add(notificationRecord);
            return notificationRecord;
        }
    }
}
