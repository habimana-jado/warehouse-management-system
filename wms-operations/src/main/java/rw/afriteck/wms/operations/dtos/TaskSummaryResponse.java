package rw.afriteck.wms.operations.dtos;

import rw.afriteck.wms.operations.enums.TaskPriority;
import rw.afriteck.wms.operations.enums.TaskStatus;
import rw.afriteck.wms.operations.enums.TaskType;

import java.time.Instant;
import java.util.UUID;

public record TaskSummaryResponse(
        UUID id, String taskNumber,
        TaskType type, TaskStatus status, TaskPriority priority,
        UUID warehouseId,
        String referenceType, UUID referenceId, String referenceNumber,
        UUID assigneeId, String assigneeName,
        Instant dueAt, Instant createdAt,
        int lineCount, int doneLineCount
) {}
