package rw.afriteck.wms.operations.dtos;

import rw.afriteck.wms.operations.enums.TaskPriority;
import rw.afriteck.wms.operations.enums.TaskStatus;
import rw.afriteck.wms.operations.enums.TaskType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TaskResponse(
        UUID id, long version, String taskNumber,
        TaskType type, TaskStatus status, TaskPriority priority,
        UUID warehouseId, UUID zoneLocationId,
        String referenceType, UUID referenceId, String referenceNumber,
        UUID assigneeId, String assigneeName,
        Instant assignedAt, Instant startedAt, Instant completedAt, Instant dueAt,
        String cancelReason, String notes, Instant createdAt,
        List<TaskLineResponse> lines
) {}
