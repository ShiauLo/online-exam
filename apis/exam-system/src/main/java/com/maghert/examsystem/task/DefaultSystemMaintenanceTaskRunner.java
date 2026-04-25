package com.maghert.examsystem.task;

import com.maghert.examsystem.context.RequestContext;
import com.maghert.examsystem.entity.SystemBackupRecordEntity;
import org.springframework.stereotype.Component;

@Component
public class DefaultSystemMaintenanceTaskRunner implements SystemMaintenanceTaskRunner {

    @Override
    public void executeBackup(SystemBackupRecordEntity backupRecord, RequestContext context) {
        // 当前阶段仅模拟后台执行闭环，真实备份编排后续再替换。
    }

    @Override
    public void executeRestore(SystemBackupRecordEntity backupRecord, RequestContext context) {
        // 当前阶段仅模拟后台执行闭环，真实恢复编排后续再替换。
    }
}
