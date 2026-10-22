package rw.afriteck.wms.web.operations;

import rw.afriteck.wms.operations.enums.TaskPriority;
import rw.afriteck.wms.operations.enums.TaskStatus;
import rw.afriteck.wms.operations.enums.TaskType;

import java.time.Instant;
import java.util.UUID;

public record TaskRow(UUID id, String taskNumber, TaskType type, TaskStatus status, TaskPriority priority,
                      String referenceNumber, String referenceUrl, String assigneeName,
                      Instant dueAt, boolean overdue, int lineCount, int doneLineCount, boolean canStart) {}

