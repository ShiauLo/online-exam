package com.maghert.examsystem.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.maghert.examsystem.entity.SystemAlarmSettingEntity;
import com.maghert.examsystem.entity.SystemAuditRecordEntity;
import com.maghert.examsystem.entity.SystemBackupRecordEntity;
import com.maghert.examsystem.entity.SystemConfigEntity;
import com.maghert.examsystem.entity.SystemLogEntity;
import com.maghert.examsystem.entity.SystemNotificationRecordEntity;
import com.maghert.examsystem.entity.SystemPermissionAssignmentEntity;
import com.maghert.examsystem.entity.SystemRoleEntity;
import com.maghert.examsystem.mapper.SystemAlarmSettingMapper;
import com.maghert.examsystem.mapper.SystemAuditRecordMapper;
import com.maghert.examsystem.mapper.SystemBackupRecordMapper;
import com.maghert.examsystem.mapper.SystemConfigMapper;
import com.maghert.examsystem.mapper.SystemLogMapper;
import com.maghert.examsystem.mapper.SystemNotificationRecordMapper;
import com.maghert.examsystem.mapper.SystemPermissionAssignmentMapper;
import com.maghert.examsystem.mapper.SystemRoleMapper;
import com.maghert.examsystem.repository.SystemDomainRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MysqlSystemDomainRepository implements SystemDomainRepository {

    private final SystemRoleMapper roleMapper;
    private final SystemPermissionAssignmentMapper assignmentMapper;
    private final SystemConfigMapper configMapper;
    private final SystemAlarmSettingMapper alarmSettingMapper;
    private final SystemLogMapper logMapper;
    private final SystemBackupRecordMapper backupRecordMapper;
    private final SystemAuditRecordMapper auditRecordMapper;
    private final SystemNotificationRecordMapper notificationRecordMapper;

    public MysqlSystemDomainRepository(SystemRoleMapper roleMapper,
                                       SystemPermissionAssignmentMapper assignmentMapper,
                                       SystemConfigMapper configMapper,
                                       SystemAlarmSettingMapper alarmSettingMapper,
                                       SystemLogMapper logMapper,
                                       SystemBackupRecordMapper backupRecordMapper,
                                       SystemAuditRecordMapper auditRecordMapper,
                                       SystemNotificationRecordMapper notificationRecordMapper) {
        this.roleMapper = roleMapper;
        this.assignmentMapper = assignmentMapper;
        this.configMapper = configMapper;
        this.alarmSettingMapper = alarmSettingMapper;
        this.logMapper = logMapper;
        this.backupRecordMapper = backupRecordMapper;
        this.auditRecordMapper = auditRecordMapper;
        this.notificationRecordMapper = notificationRecordMapper;
    }

    @Override
    public SystemRoleEntity saveRole(SystemRoleEntity role) {
        roleMapper.insert(role);
        return role;
    }

    @Override
    public void updateRole(SystemRoleEntity role) {
        roleMapper.updateById(role);
    }

    @Override
    public Optional<SystemRoleEntity> findRoleById(Long roleId) {
        return Optional.ofNullable(roleMapper.selectById(roleId));
    }

    @Override
    public List<SystemRoleEntity> listRoles() {
        return roleMapper.selectList(null);
    }

    @Override
    public SystemPermissionAssignmentEntity saveAssignment(SystemPermissionAssignmentEntity assignment) {
        assignmentMapper.insert(assignment);
        return assignment;
    }

    @Override
    public void updateAssignment(SystemPermissionAssignmentEntity assignment) {
        assignmentMapper.updateById(assignment);
    }

    @Override
    public Optional<SystemPermissionAssignmentEntity> findAssignmentByAccountId(Long accountId) {
        return Optional.ofNullable(assignmentMapper.selectOne(new LambdaQueryWrapper<SystemPermissionAssignmentEntity>()
                .eq(SystemPermissionAssignmentEntity::getAccountId, accountId)));
    }

    @Override
    public List<SystemPermissionAssignmentEntity> listAssignments() {
        return assignmentMapper.selectList(null);
    }

    @Override
    public SystemConfigEntity saveConfig(SystemConfigEntity config) {
        configMapper.insert(config);
        return config;
    }

    @Override
    public void updateConfig(SystemConfigEntity config) {
        configMapper.updateById(config);
    }

    @Override
    public Optional<SystemConfigEntity> findConfigByKey(String configKey) {
        return Optional.ofNullable(configMapper.selectOne(new LambdaQueryWrapper<SystemConfigEntity>()
                .eq(SystemConfigEntity::getConfigKey, configKey)));
    }

    @Override
    public List<SystemConfigEntity> listConfigs() {
        return configMapper.selectList(null);
    }

    @Override
    public SystemAlarmSettingEntity saveAlarm(SystemAlarmSettingEntity alarmSetting) {
        alarmSettingMapper.insert(alarmSetting);
        return alarmSetting;
    }

    @Override
    public void updateAlarm(SystemAlarmSettingEntity alarmSetting) {
        alarmSettingMapper.updateById(alarmSetting);
    }

    @Override
    public Optional<SystemAlarmSettingEntity> findAlarmByType(String alarmType) {
        return Optional.ofNullable(alarmSettingMapper.selectOne(new LambdaQueryWrapper<SystemAlarmSettingEntity>()
                .eq(SystemAlarmSettingEntity::getAlarmType, alarmType)));
    }

    @Override
    public List<SystemAlarmSettingEntity> listAlarms() {
        return alarmSettingMapper.selectList(null);
    }

    @Override
    public SystemLogEntity saveLog(SystemLogEntity log) {
        logMapper.insert(log);
        return log;
    }

    @Override
    public List<SystemLogEntity> listLogs() {
        return logMapper.selectList(null);
    }

    @Override
    public SystemBackupRecordEntity saveBackup(SystemBackupRecordEntity backupRecord) {
        backupRecordMapper.insert(backupRecord);
        return backupRecord;
    }

    @Override
    public void updateBackup(SystemBackupRecordEntity backupRecord) {
        backupRecordMapper.updateById(backupRecord);
    }

    @Override
    public Optional<SystemBackupRecordEntity> findBackupById(Long backupId) {
        return Optional.ofNullable(backupRecordMapper.selectById(backupId));
    }

    @Override
    public List<SystemBackupRecordEntity> listBackups() {
        return backupRecordMapper.selectList(null);
    }

    @Override
    public void saveAudit(SystemAuditRecordEntity auditRecord) {
        auditRecordMapper.insert(auditRecord);
    }

    @Override
    public SystemNotificationRecordEntity saveNotification(SystemNotificationRecordEntity notificationRecord) {
        notificationRecordMapper.insert(notificationRecord);
        return notificationRecord;
    }
}
