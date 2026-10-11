package rw.afriteck.wms.operations.dtos;

import rw.afriteck.wms.operations.enums.TaskEventType;
import rw.afriteck.wms.operations.enums.TaskStatus;

import java.time.Instant;
import java.util.UUID;

public record TaskEventResponse(
        UUID id, TaskEventType eventType,
        TaskStatus fromStatus, TaskStatus toStatus,
        UUID actorUserId, String actorName,
        UUID assigneeId, String assigneeName,
        Instant occurredAt, String note
) {}
