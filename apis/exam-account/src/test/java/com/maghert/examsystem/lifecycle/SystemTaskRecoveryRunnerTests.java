package com.maghert.examsystem.lifecycle;

import com.maghert.examsystem.service.impl.SystemServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.BadSqlGrammarException;

import java.sql.SQLSyntaxErrorException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SystemTaskRecoveryRunnerTests {

    @Test
    void shouldIgnoreMissingSystemBackupRecordTable() {
        SystemServiceImpl systemService = mock(SystemServiceImpl.class);
        doThrow(new BadSqlGrammarException(
                "select",
                "SELECT * FROM system_backup_record",
                new SQLSyntaxErrorException("Table 'online_exam_db.system_backup_record' doesn't exist")))
                .when(systemService)
                .recoverInterruptedBackupTasks();

        SystemTaskRecoveryRunner runner = new SystemTaskRecoveryRunner(systemService);

        assertDoesNotThrow(() -> runner.run(new DefaultApplicationArguments(new String[0])));
        verify(systemService).recoverInterruptedBackupTasks();
    }

    @Test
    void shouldRethrowOtherSqlGrammarProblems() {
        SystemServiceImpl systemService = mock(SystemServiceImpl.class);
        BadSqlGrammarException exception = new BadSqlGrammarException(
                "select",
                "SELECT * FROM system_role",
                new SQLSyntaxErrorException("Table 'online_exam_db.system_role' doesn't exist"));
        doThrow(exception)
                .when(systemService)
                .recoverInterruptedBackupTasks();

        SystemTaskRecoveryRunner runner = new SystemTaskRecoveryRunner(systemService);

        BadSqlGrammarException thrown = assertThrows(BadSqlGrammarException.class,
                () -> runner.run(new DefaultApplicationArguments(new String[0])));
        verify(systemService).recoverInterruptedBackupTasks();
        org.junit.jupiter.api.Assertions.assertSame(exception, thrown);
    }
}
