package rw.afriteck.wms.operations.enums;

import java.util.EnumSet;
import java.util.Set;

public enum TaskStatus {
    OPEN, ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED;

    public static final Set<TaskStatus> ACTIVE = EnumSet.of(OPEN, ASSIGNED, IN_PROGRESS);
}