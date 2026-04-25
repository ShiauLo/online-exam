package com.maghert.examsystem.repository;

import com.maghert.examsystem.entity.SystemAlarmSettingEntity;
import com.maghert.examsystem.entity.SystemAuditRecordEntity;
import com.maghert.examsystem.entity.SystemBackupRecordEntity;
import com.maghert.examsystem.entity.SystemConfigEntity;
import com.maghert.examsystem.entity.SystemLogEntity;
import com.maghert.examsystem.entity.SystemNotificationRecordEntity;
import com.maghert.examsystem.entity.SystemPermissionAssignmentEntity;
import com.maghert.examsystem.entity.SystemRoleEntity;

import java.util.List;
import java.util.Optional;

public interface SystemDomainRepository {

    SystemRoleEntity saveRole(SystemRoleEntity role);

    void updateRole(SystemRoleEntity role);

    Optional<SystemRoleEntity> findRoleById(Long roleId);

    List<SystemRoleEntity> listRoles();

    SystemPermissionAssignmentEntity saveAssignment(SystemPermissionAssignmentEntity assignment);

    void updateAssignment(SystemPermissionAssignmentEntity assignment);

    Optional<SystemPermissionAssignmentEntity> findAssignmentByAccountId(Long accountId);

    List<SystemPermissionAssignmentEntity> listAssignments();

    SystemConfigEntity saveConfig(SystemConfigEntity config);

    void updateConfig(SystemConfigEntity config);

    Optional<SystemConfigEntity> findConfigByKey(String configKey);

    List<SystemConfigEntity> listConfigs();

    SystemAlarmSettingEntity saveAlarm(SystemAlarmSettingEntity alarmSetting);

    void updateAlarm(SystemAlarmSettingEntity alarmSetting);

    Optional<SystemAlarmSettingEntity> findAlarmByType(String alarmType);

    List<SystemAlarmSettingEntity> listAlarms();

    SystemLogEntity saveLog(SystemLogEntity log);

    List<SystemLogEntity> listLogs();

    SystemBackupRecordEntity saveBackup(SystemBackupRecordEntity backupRecord);

    void updateBackup(SystemBackupRecordEntity backupRecord);

    Optional<SystemBackupRecordEntity> findBackupById(Long backupId);

    List<SystemBackupRecordEntity> listBackups();

    void saveAudit(SystemAuditRecordEntity auditRecord);

    SystemNotificationRecordEntity saveNotification(SystemNotificationRecordEntity notificationRecord);
}
