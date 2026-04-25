package com.maghert.examsystem.lifecycle;

import com.maghert.examsystem.service.impl.SystemServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class SystemTaskRecoveryRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SystemTaskRecoveryRunner.class);

    private final SystemServiceImpl systemService;

    public SystemTaskRecoveryRunner(SystemServiceImpl systemService) {
        this.systemService = systemService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            systemService.recoverInterruptedBackupTasks();
        } catch (BadSqlGrammarException exception) {
            if (isMissingBackupRecordTable(exception)) {
                log.warn("检测到数据库缺少 system_backup_record 表，已跳过备份恢复任务。请尽快导入最新 docs/sql/mysql.sql。", exception);
                return;
            }
            throw exception;
        }
    }

    private boolean isMissingBackupRecordTable(BadSqlGrammarException exception) {
        BadSqlGrammarException sqlException = Objects.requireNonNull(exception);
        Throwable rootCause = NestedExceptionUtils.getMostSpecificCause(sqlException);
        Throwable resolvedCause = rootCause != null ? rootCause : sqlException;
        String message = resolvedCause.getMessage();
        return message != null && message.contains("system_backup_record");
    }
}
