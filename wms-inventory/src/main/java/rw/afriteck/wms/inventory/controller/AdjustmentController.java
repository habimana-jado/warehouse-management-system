package rw.afriteck.wms.inventory.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.ForbiddenException;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.inventory.dtos.*;
import rw.afriteck.wms.inventory.enums.AdjustmentStatus;
import rw.afriteck.wms.inventory.service.AdjustmentService;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wms/inventory/adjustments")
@RequiredArgsConstructor
public class AdjustmentController {

    private final AdjustmentService service;
    private final UserContext userContext;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(InventoryAccess.WORKER)
    public AdjustmentResponse create(@RequestBody CreateAdjustmentRequest request) {
        userContext.requireWarehouseAccess(request.warehouseId());
        return service.create(request, userContext.currentUserId());
    }

    @GetMapping
    @PreAuthorize(InventoryAccess.BOARD)
    public PageResponse<AdjustmentSummaryResponse> search(@RequestParam UUID warehouseId,
                                                          @RequestParam(required = false) Set<AdjustmentStatus> status,
                                                          @RequestParam(required = false) String q,
                                                          Pageable pageable) {
        userContext.requireWarehouseAccess(warehouseId);
        return service.search(new AdjustmentFilter(warehouseId, status, q, null), pageable);
    }

    /** The caller's own adjustments (what receivers and pickers see). */
    @GetMapping("/my")
    @PreAuthorize(InventoryAccess.WORKER)
    public PageResponse<AdjustmentSummaryResponse> mine(@RequestParam UUID warehouseId,
                                                        @RequestParam(required = false) Set<AdjustmentStatus> status,
                                                        @RequestParam(required = false) String q,
                                                        Pageable pageable) {
        userContext.requireWarehouseAccess(warehouseId);
        return service.search(new AdjustmentFilter(warehouseId, status, q, userContext.currentUserId()), pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(InventoryAccess.ANY_USER)
    public AdjustmentResponse get(@PathVariable UUID id) {
        AdjustmentAccessInfo info = service.accessInfo(id);
        userContext.requireWarehouseAccess(info.warehouseId());
        if (workerOnly() && !userContext.currentUserId().equals(info.requestedBy())) {
            throw new ForbiddenException("Workers can only view their own adjustments");
        }
        return service.getById(id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(InventoryAccess.MANAGER)
    public AdjustmentResponse approve(@PathVariable UUID id) {
        userContext.requireWarehouseAccess(service.accessInfo(id).warehouseId());
        return service.approve(id, userContext.currentUserId());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize(InventoryAccess.MANAGER)
    public AdjustmentResponse reject(@PathVariable UUID id, @RequestBody RejectAdjustmentRequest request) {
        userContext.requireWarehouseAccess(service.accessInfo(id).warehouseId());
        return service.reject(id, userContext.currentUserId(), request.reason());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(InventoryAccess.WORKER)
    public AdjustmentResponse cancel(@PathVariable UUID id) {
        userContext.requireWarehouseAccess(service.accessInfo(id).warehouseId());
        return service.cancel(id, userContext.currentUserId());        // requester-only is enforced in the service
    }

    private boolean workerOnly() {
        return !(userContext.isAdmin() || userContext.hasRole("WAREHOUSE_MANAGER") || userContext.hasRole("VIEWER"));
    }
}
