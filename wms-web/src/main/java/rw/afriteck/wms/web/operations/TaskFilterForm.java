package rw.afriteck.wms.web.operations;

import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.operations.dtos.TaskFilter;
import rw.afriteck.wms.operations.enums.TaskPriority;
import rw.afriteck.wms.operations.enums.TaskStatus;
import rw.afriteck.wms.operations.enums.TaskType;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class TaskFilterForm {
    public static final String STATUS_ALL = "ALL";

    private String q;
    private String status;                 // blank = active tasks, "ALL" = everything, else one TaskStatus name
    private TaskType type;
    private TaskPriority priority;
    private boolean unassignedOnly;

    public TaskFilter toFilter(UUID warehouseId, UUID assigneeId) {
        return new TaskFilter(warehouseId, statuses(), type == null ? null : Set.of(type), priority,
                assigneeId, assigneeId == null && unassignedOnly, null, null, Codes.blankToNull(q));
    }

    private Set<TaskStatus> statuses() {
        if (status == null || status.isBlank()) return TaskStatus.ACTIVE;
        if (STATUS_ALL.equals(status)) return null;
        try {
            return EnumSet.of(TaskStatus.valueOf(status));
        } catch (IllegalArgumentException e) {
            return TaskStatus.ACTIVE;
        }
    }
}

