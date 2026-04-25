package com.maghert.examsystem.controller;

import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maghert.examsystem.context.RequestContext;
import com.maghert.examsystem.context.RequestContextResolver;
import com.maghert.examsystem.entity.SystemAlarmSettingEntity;
import com.maghert.examsystem.entity.SystemAuditRecordEntity;
import com.maghert.examsystem.entity.SystemBackupRecordEntity;
import com.maghert.examsystem.entity.SystemConfigEntity;
import com.maghert.examsystem.entity.SystemLogEntity;
import com.maghert.examsystem.entity.SystemNotificationRecordEntity;
import com.maghert.examsystem.entity.SystemPermissionAssignmentEntity;
import com.maghert.examsystem.entity.SystemRoleEntity;
import com.maghert.examsystem.handler.GlobalExceptionHandler;
import com.maghert.examsystem.model.dto.DataBackupRequest;
import com.maghert.examsystem.model.dto.DataQueryRequest;
import com.maghert.examsystem.model.dto.DataRestoreRequest;
import com.maghert.examsystem.model.dto.PermissionQueryRequest;
import com.maghert.examsystem.model.enums.BackupTaskStatus;
import com.maghert.examsystem.notification.OutboxSystemNotificationPublisher;
import com.maghert.examsystem.notification.SystemNotificationPublisher;
import com.maghert.examsystem.repository.SystemDomainRepository;
import com.maghert.examsystem.service.impl.SystemServiceImpl;
import com.maghert.examsystem.task.SystemMaintenanceTaskRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SystemControllerPermissionTests {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private InMemorySystemDomainRepository repository;
    java.nio.file.Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        repository = new InMemorySystemDomainRepository();
        ManualTaskExecutor taskExecutor = new ManualTaskExecutor();
        tempDir = java.nio.file.Files.createDirectories(
                java.nio.file.Path.of("target", "system-controller-tests", Long.toString(System.nanoTime())));
        SystemNotificationPublisher notificationPublisher =
                new OutboxSystemNotificationPublisher(repository, new Snowflake(2, 1), new ObjectMapper());
        SystemServiceImpl systemService = new SystemServiceImpl(
                new Snowflake(1, 1),
                repository,
                taskExecutor,
                notificationPublisher,
                new NoOpMaintenanceTaskRunner(),
                tempDir.toString());
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
        ReflectionTestUtils.setField(exceptionHandler, "activeEnv", "dev");
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new SystemController(systemService, new RequestContextResolver()))
                .setValidator(validator)
                .setControllerAdvice(exceptionHandler)
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldReturnTeacherPermissionTemplate() throws Exception {
        PermissionQueryRequest request = new PermissionQueryRequest();

        mockMvc.perform(post("/api/system/permission/query")
                        .header("X-User-Id", "1001")
                        .header("X-Role-Id", "3")
                        .header("X-Request-Id", "req-1")
                        .contentType(Objects.requireNonNull(APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("success"))
                .andExpect(jsonPath("$.requestId").value("req-1"))
                .andExpect(jsonPath("$.data.roleType").value("teacher"))
                .andExpect(jsonPath("$.data.routeList[0].path").value("/teacher"))
                .andExpect(jsonPath("$.data.routeList[2].path").value("/teacher/question"))
                .andExpect(jsonPath("$.data.buttonList[4]").value("question.query"));
    }

    @Test
    void shouldReturnStudentPermissionTemplateWithRealtimeRoutes() throws Exception {
        PermissionQueryRequest request = new PermissionQueryRequest();

        mockMvc.perform(post("/api/system/permission/query")
                        .header("X-User-Id", "1004")
                        .header("X-Role-Id", "4")
                        .header("X-Request-Id", "req-student")
                        .contentType(Objects.requireNonNull(APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.requestId").value("req-student"))
                .andExpect(jsonPath("$.data.roleType").value("student"))
                .andExpect(jsonPath("$.data.routeList[0].path").value("/student"))
                .andExpect(jsonPath("$.data.routeList[3].path").value("/student/exam/answer"))
                .andExpect(jsonPath("$.data.buttonList[2]").value("exam.enter"))
                .andExpect(jsonPath("$.data.buttonList[4]").value("exam.submit"));
    }

    @Test
    void shouldReturnAcceptedPayloadWhenBackupRequested() throws Exception {
        DataBackupRequest request = new DataBackupRequest();
        request.setBackupType("full");
        request.setRemark("nightly");

        mockMvc.perform(post("/api/system/data/backup")
                        .header("X-User-Id", "6001")
                        .header("X-Role-Id", "6")
                        .header("X-Request-Id", "req-backup")
                        .contentType(Objects.requireNonNull(APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.requestId").value("req-backup"))
                .andExpect(jsonPath("$.data.status").value("BACKUP_PENDING"))
                .andExpect(jsonPath("$.data.lifecycleStage").value("accepted"));
    }

    @Test
    void shouldReturnAcceptedPayloadWhenRestoreRequested() throws Exception {
        repository.saveBackup(new SystemBackupRecordEntity()
                .setBackupId(1L)
                .setBackupType("full")
                .setStatus(BackupTaskStatus.BACKUP_SUCCESS.name())
                .setOperatorId(6001L)
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now()));
        DataRestoreRequest request = new DataRestoreRequest();
        request.setBackupId(1L);
        request.setVerifyCode1("CONFIRM_SYSTEM_RESTORE");
        request.setVerifyCode2("CONFIRM_SYSTEM_RESTORE");

        mockMvc.perform(post("/api/system/data/restore")
                        .header("X-User-Id", "1")
                        .header("X-Role-Id", "1")
                        .header("X-Request-Id", "req-restore")
                        .contentType(Objects.requireNonNull(APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.requestId").value("req-restore"))
                .andExpect(jsonPath("$.data.status").value("RESTORE_PENDING"))
                .andExpect(jsonPath("$.data.lifecycleStage").value("accepted"));
    }

    @Test
    void shouldReturnLifecycleStageWhenQueryBackups() throws Exception {
        repository.saveBackup(new SystemBackupRecordEntity()
                .setBackupId(1L)
                .setBackupType("full")
                .setStatus(BackupTaskStatus.RESTORE_FAILED.name())
                .setOperatorId(6001L)
                .setCreateTime(LocalDateTime.of(2026, 4, 16, 10, 0, 0))
                .setUpdateTime(LocalDateTime.of(2026, 4, 16, 10, 5, 0)));
        DataQueryRequest request = new DataQueryRequest();

        mockMvc.perform(post("/api/system/data/query")
                        .header("X-User-Id", "1")
                        .header("X-Role-Id", "1")
                        .header("X-Request-Id", "req-query")
                        .contentType(Objects.requireNonNull(APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.requestId").value("req-query"))
                .andExpect(jsonPath("$.data.records[0].status").value("RESTORE_FAILED"))
                .andExpect(jsonPath("$.data.records[0].lifecycleStage").value("failed"))
                .andExpect(jsonPath("$.data.records[0].canRestore").value(true));
    }

    private static final class ManualTaskExecutor implements TaskExecutor {

        @Override
        public void execute(Runnable task) {
            // 控制器测试只验证受理响应，不主动推进后台任务。
        }
    }

    private static final class NoOpMaintenanceTaskRunner implements SystemMaintenanceTaskRunner {

        @Override
        public void executeBackup(SystemBackupRecordEntity backupRecord, RequestContext context) {
        }

        @Override
        public void executeRestore(SystemBackupRecordEntity backupRecord, RequestContext context) {
        }
    }

    private static final class InMemorySystemDomainRepository implements SystemDomainRepository {

        private final List<SystemRoleEntity> roles = new ArrayList<>();
        private final List<SystemPermissionAssignmentEntity> assignments = new ArrayList<>();
        private final List<SystemConfigEntity> configs = new ArrayList<>();
        private final List<SystemAlarmSettingEntity> alarms = new ArrayList<>();
        private final List<SystemLogEntity> logs = new ArrayList<>();
        private final List<SystemBackupRecordEntity> backups = new ArrayList<>();

        @Override
        public SystemRoleEntity saveRole(SystemRoleEntity role) {
            roles.add(role);
            return role;
        }

        @Override
        public void updateRole(SystemRoleEntity role) {
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
            assignments.add(assignment);
            return assignment;
        }

        @Override
        public void updateAssignment(SystemPermissionAssignmentEntity assignment) {
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
            configs.add(config);
            return config;
        }

        @Override
        public void updateConfig(SystemConfigEntity config) {
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
            alarms.add(alarmSetting);
            return alarmSetting;
        }

        @Override
        public void updateAlarm(SystemAlarmSettingEntity alarmSetting) {
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
        }

        @Override
        public SystemNotificationRecordEntity saveNotification(SystemNotificationRecordEntity notificationRecord) {
            return notificationRecord;
        }
    }
}
