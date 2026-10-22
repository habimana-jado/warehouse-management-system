package rw.afriteck.wms.web.operations;

import rw.afriteck.wms.operations.dtos.TaskEventResponse;
import rw.afriteck.wms.operations.dtos.TaskResponse;

import java.util.List;

public record TaskView(TaskResponse task, String referenceUrl, boolean overdue,
                       boolean canStart, boolean canRelease, boolean canAssign,
                       boolean canCancel, boolean canChangePriority,
                       List<TaskEventResponse> events) {}
