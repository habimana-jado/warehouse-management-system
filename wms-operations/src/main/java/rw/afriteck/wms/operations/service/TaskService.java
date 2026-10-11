package rw.afriteck.wms.operations.service;

import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.operations.dtos.*;
import rw.afriteck.wms.operations.enums.TaskPriority;
import rw.afriteck.wms.operations.enums.TaskType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface TaskService {

    // ---- used by inbound / outbound / inventory ----
    TaskResponse create(CreateTaskRequest r);

    TaskResponse getById(UUID id);

    List<TaskResponse> findByReference(String referenceType, UUID referenceId);

    TaskResponse completeLine(UUID taskId, UUID lineId, BigDecimal doneQty, String shortReason);

    TaskResponse cancel(UUID taskId, String reason);

    int cancelByReference(String referenceType, UUID referenceId, String reason);

    // ---- operations' own controllers ----
    PageResponse<TaskSummaryResponse> search(TaskFilter filter, Pageable pageable);

    List<TaskEventResponse> history(UUID taskId);

    TaskResponse assign(UUID taskId, UUID assigneeId);       // also covers reassign

    TaskResponse release(UUID taskId);                       // back to OPEN, assignee cleared

    TaskResponse start(UUID taskId);                         // ASSIGNED -> IN_PROGRESS

    TaskResponse changePriority(UUID taskId, TaskPriority priority);

    Optional<TaskResponse> claimNext(UUID warehouseId, Set<TaskType> types, UUID userId);

    TaskAccessInfo accessInfo(UUID taskId);       // NotFoundException if missing
}
