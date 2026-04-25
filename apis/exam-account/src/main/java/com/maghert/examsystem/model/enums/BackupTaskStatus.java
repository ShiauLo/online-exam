package com.maghert.examsystem.model.enums;

import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;

public enum BackupTaskStatus {

    BACKUP_PENDING("accepted", false),
    BACKUP_RUNNING("running", false),
    BACKUP_SUCCESS("available", true),
    BACKUP_FAILED("failed", false),
    RESTORE_PENDING("accepted", false),
    RESTORE_RUNNING("running", false),
    RESTORE_SUCCESS("completed", false),
    RESTORE_FAILED("failed", true);

    private static final EnumSet<BackupTaskStatus> RECOVERABLE_RUNNING_STATUSES = EnumSet.of(
            BACKUP_PENDING, BACKUP_RUNNING, RESTORE_PENDING, RESTORE_RUNNING);

    private final String lifecycleStage;
    private final boolean canRestore;

    BackupTaskStatus(String lifecycleStage, boolean canRestore) {
        this.lifecycleStage = lifecycleStage;
        this.canRestore = canRestore;
    }

    public String getLifecycleStage() {
        return lifecycleStage;
    }

    public boolean canRestore() {
        return canRestore;
    }

    public boolean isRestoreEnterable() {
        return this == BACKUP_SUCCESS || this == RESTORE_FAILED;
    }

    public boolean isRecoverableRunningStatus() {
        return RECOVERABLE_RUNNING_STATUSES.contains(this);
    }

    public BackupTaskStatus toRecoveredFailureStatus() {
        return switch (this) {
            case BACKUP_PENDING, BACKUP_RUNNING -> BACKUP_FAILED;
            case RESTORE_PENDING, RESTORE_RUNNING -> RESTORE_FAILED;
            default -> this;
        };
    }

    public static Optional<BackupTaskStatus> find(String value) {
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(item -> item.name().equals(normalized))
                .findFirst();
    }

    public static boolean canRestore(String value) {
        return find(value).map(BackupTaskStatus::canRestore).orElse(false);
    }

    public static String resolveLifecycleStage(String value) {
        return find(value)
                .map(BackupTaskStatus::getLifecycleStage)
                .orElse("unknown");
    }
}
