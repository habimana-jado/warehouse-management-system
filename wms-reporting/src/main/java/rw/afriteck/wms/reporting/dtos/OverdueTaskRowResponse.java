package rw.afriteck.wms.reporting.dtos;

import java.time.Instant;
import java.util.UUID;

public record OverdueTaskRowResponse(UUID id, String taskNumber, String type, String status, String priority,
                                     String referenceType, String referenceNumber, UUID assigneeId, String assigneeName,
                                     Instant dueAt, long hoursOverdue, Instant createdAt) {}
