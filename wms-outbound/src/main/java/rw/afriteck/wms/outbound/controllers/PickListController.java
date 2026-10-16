package rw.afriteck.wms.outbound.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.ForbiddenException;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.operations.service.TaskService;
import rw.afriteck.wms.outbound.dtos.CancelPickListRequest;
import rw.afriteck.wms.outbound.dtos.ConfirmPickRequest;
import rw.afriteck.wms.outbound.dtos.PickListResponse;
import rw.afriteck.wms.outbound.dtos.PickListSummaryResponse;
import rw.afriteck.wms.outbound.enums.PickListStatus;
import rw.afriteck.wms.outbound.service.PickListService;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wms/outbound/pick-lists")
@RequiredArgsConstructor
public class PickListController {

    private final PickListService pickLists;
    private final TaskService taskService;          // accessInfo: who is the task assigned to
    private final UserContext userContext;

    @GetMapping
    @PreAuthorize(OutboundAccess.READ)
    public PageResponse<PickListSummaryResponse> search(@RequestParam UUID warehouseId,
                                                        @RequestParam(required = false) Set<PickListStatus> statuses,
                                                        Pageable pageable) {
        userContext.requireWarehouseAccess(warehouseId);
        return pickLists.search(warehouseId, statuses, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(OutboundAccess.PICK_READ)
    public PickListResponse get(@PathVariable UUID id) {
        userContext.requireWarehouseAccess(pickLists.warehouseIdOf(id));
        return checkedRead(pickLists.getById(id));
    }

    @GetMapping("/by-task/{taskId}")
    @PreAuthorize(OutboundAccess.PICK_READ)
    public PickListResponse byTask(@PathVariable UUID taskId) {
        PickListResponse p = pickLists.getByTaskId(taskId);
        userContext.requireWarehouseAccess(p.warehouseId());
        return checkedRead(p);
    }

    @PostMapping("/{id}/lines/{lineId}/confirm")
    @PreAuthorize(OutboundAccess.PICK)
    public PickListResponse confirm(@PathVariable UUID id, @PathVariable UUID lineId,
                                    @RequestBody ConfirmPickRequest request) {
        userContext.requireWarehouseAccess(pickLists.warehouseIdOf(id));
        // the service enforces "non-privileged callers must be the task assignee" and the manager-only expiry override
        return pickLists.confirmPick(id, lineId, request, userContext.currentUserId(), Callers.privileged(userContext));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(OutboundAccess.MANAGER)
    public PickListResponse cancel(@PathVariable UUID id, @RequestBody CancelPickListRequest request) {
        userContext.requireWarehouseAccess(pickLists.warehouseIdOf(id));
        return pickLists.cancel(id, request);
    }

    /** Managers and viewers read any list of their warehouses; a picker reads only the list of a task assigned to them. */
    private PickListResponse checkedRead(PickListResponse p) {
        if (Callers.privileged(userContext) || Callers.viewer(userContext)) return p;
        UUID assignee = p.taskId() == null ? null : taskService.accessInfo(p.taskId()).assigneeId();
        if (!userContext.currentUserId().equals(assignee)) {
            throw new ForbiddenException("This pick list is assigned to another user");
        }
        return p;
    }
}