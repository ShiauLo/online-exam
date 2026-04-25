package com.maghert.examsystem.task;

import com.maghert.examsystem.context.RequestContext;
import com.maghert.examsystem.entity.SystemBackupRecordEntity;

public interface SystemMaintenanceTaskRunner {

    void executeBackup(SystemBackupRecordEntity backupRecord, RequestContext context);

    void executeRestore(SystemBackupRecordEntity backupRecord, RequestContext context);
}
