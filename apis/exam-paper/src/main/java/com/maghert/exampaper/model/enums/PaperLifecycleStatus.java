package com.maghert.exampaper.model.enums;

public enum PaperLifecycleStatus {
    DRAFT,
    APPROVED,
    REJECTED,
    PUBLISHED,
    TERMINATED,
    RECYCLED;

    public boolean editable() {
        return this == DRAFT || this == REJECTED;
    }

    public boolean publishable() {
        return this == DRAFT || this == APPROVED;
    }
}
