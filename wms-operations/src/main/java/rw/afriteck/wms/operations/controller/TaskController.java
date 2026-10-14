package rw.afriteck.wms.operations.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.ForbiddenException;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.operations.dtos.*;
import rw.afriteck.wms.operations.enums.TaskStatus;
import rw.afriteck.wms.operations.enums.TaskType;
import rw.afriteck.wms.operations.service.TaskService;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wms/operation/tasks")
@RequiredArgsConstructor
public class TaskController {

    private static final Set<String> SORTABLE =
            Set.of("priorityRank", "dueAt", "createdAt", "taskNumber", "status");

    private final TaskService taskService;
    private final UserContext userContext;

    // ------------------------------------------------------------ queries

    /** Task board for a warehouse (managers, admins, viewers). */
    @GetMapping
    @PreAuthorize(OperationsAccess.BOARD)
    public PageResponse<TaskSummaryResponse> board(@Valid TaskFilter filter,
                                                   @PageableDefault(size = 20) Pageable pageable) {
        userContext.requireWarehouseAccess(filter.warehouseId());
        return taskService.search(filter, checkSort(pageable));
    }

    /** The caller's own tasks. Defaults to ASSIGNED and IN_PROGRESS. */
    @GetMapping("/my")
    @PreAuthorize(OperationsAccess.WORKER)
    public PageResponse<TaskSummaryResponse> myTasks(@RequestParam UUID warehouseId,
                                                     @RequestParam(required = false) Set<TaskStatus> statuses,
                                                     @RequestParam(required = false) Set<TaskType> types,
                                                     @PageableDefault(size = 20) Pageable pageable) {
        userContext.requireWarehouseAccess(warehouseId);
        Set<TaskStatus> effective = (statuses == null || statuses.isEmpty())
                ? EnumSet.of(TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS) : statuses;
        TaskFilter filter = new TaskFilter(warehouseId, effective, types, null,
                userContext.currentUserId(), false, null, null, null);
        return taskService.search(filter, checkSort(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(OperationsAccess.ANY_USER)
    public TaskResponse get(@PathVariable UUID id) {
        requireReadAccess(authorize(id));
        return taskService.getById(id);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize(OperationsAccess.ANY_USER)
    public List<TaskEventResponse> history(@PathVariable UUID id) {
        requireReadAccess(authorize(id));
        return taskService.history(id);
    }

    // ------------------------------------------------------------ worker actions

    /** Scanner flow: assigns the next open task to the caller. 204 when nothing is waiting. */
    @PostMapping("/claim-next")
    @PreAuthorize(OperationsAccess.WORKER)
    public ResponseEntity<TaskResponse> claimNext(@RequestParam UUID warehouseId,
                                                  @RequestParam(required = false) Set<TaskType> types) {
        userContext.requireWarehouseAccess(warehouseId);
        return taskService.claimNext(warehouseId, types, userContext.currentUserId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{id}/start")
    @PreAuthorize(OperationsAccess.WORKER)
    public TaskResponse start(@PathVariable UUID id) {
        requireAssigneeOrManager(authorize(id));
        return taskService.start(id);
    }

    @PostMapping("/{id}/release")
    @PreAuthorize(OperationsAccess.WORKER)
    public TaskResponse release(@PathVariable UUID id) {
        requireAssigneeOrManager(authorize(id));
        return taskService.release(id);
    }

    // ------------------------------------------------------------ manager actions

    @PostMapping("/{id}/assign")
    @PreAuthorize(OperationsAccess.MANAGER)
    public TaskResponse assign(@PathVariable UUID id, @Valid @RequestBody AssignTaskRequest request) {
        authorize(id);
        return taskService.assign(id, request.assigneeId());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(OperationsAccess.MANAGER)
    public TaskResponse cancel(@PathVariable UUID id, @Valid @RequestBody CancelTaskRequest request) {
        authorize(id);
        return taskService.cancel(id, request.reason());
    }

    @PatchMapping("/{id}/priority")
    @PreAuthorize(OperationsAccess.MANAGER)
    public TaskResponse changePriority(@PathVariable UUID id,
                                       @Valid @RequestBody ChangePriorityRequest request) {
        authorize(id);
        return taskService.changePriority(id, request.priority());
    }

    // ------------------------------------------------------------ helpers

    /** Loads the minimal task facts and enforces warehouse access. */
    private TaskAccessInfo authorize(UUID taskId) {
        TaskAccessInfo info = taskService.accessInfo(taskId);
        userContext.requireWarehouseAccess(info.warehouseId());
        return info;
    }

    private boolean isManagerOrAdmin() {
        return userContext.isAdmin() || userContext.hasRole("WAREHOUSE_MANAGER");
    }

    private boolean isAssignee(TaskAccessInfo info) {
        return info.assigneeId() != null && info.assigneeId().equals(userContext.currentUserId());
    }

    /** Managers, admins and viewers read any task in their warehouses; workers only their own. */
    private void requireReadAccess(TaskAccessInfo info) {
        if (isManagerOrAdmin() || userContext.hasRole("VIEWER") || isAssignee(info)) return;
        throw new ForbiddenException("This task is not assigned to you");
    }

    private void requireAssigneeOrManager(TaskAccessInfo info) {
        if (isManagerOrAdmin() || isAssignee(info)) return;
        throw new ForbiddenException("This task is not assigned to you");
    }

    /** Reject unknown sort properties with a clean error instead of a 500 from JPA. */
    private Pageable checkSort(Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!SORTABLE.contains(order.getProperty())) {
                throw new BusinessException("INVALID_SORT",
                        "Cannot sort by '" + order.getProperty() + "'. Allowed: " + SORTABLE);
            }
        });
        return pageable;
    }
}
