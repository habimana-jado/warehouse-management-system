package rw.afriteck.wms.operations.dtos;

import jakarta.validation.constraints.NotNull;
import rw.afriteck.wms.operations.enums.TaskPriority;
import rw.afriteck.wms.operations.enums.TaskStatus;
import rw.afriteck.wms.operations.enums.TaskType;

import java.util.Set;
import java.util.UUID;

public record TaskFilter(
        @NotNull UUID warehouseId,
        Set<TaskStatus> statuses,                    // null/empty = all
        Set<TaskType> types,
        TaskPriority priority,
        UUID assigneeId,                             // "my tasks": controller sets it to the current user
        boolean unassignedOnly,
        String referenceType,
        UUID referenceId,
        String q                                     // task number or reference number
) {}
