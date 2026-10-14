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
import rw.afriteck.wms.inventory.enums.TransferStatus;
import rw.afriteck.wms.inventory.enums.TransferType;
import rw.afriteck.wms.inventory.service.TransferService;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wms/inventory/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService service;
    private final UserContext userContext;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(InventoryAccess.WORKER)
    public TransferResponse create(@RequestBody CreateTransferRequest request) {
        boolean immediateBin = request.type() == TransferType.BIN && request.immediate();
        if (!immediateBin && !isManager()) {
            throw new ForbiddenException("Only managers can plan or ship transfers");
        }
        userContext.requireWarehouseAccess(request.sourceWarehouseId());
        return service.create(request);
    }

    @GetMapping
    @PreAuthorize(InventoryAccess.BOARD)
    public PageResponse<TransferSummaryResponse> search(@RequestParam UUID warehouseId,
                                                        @RequestParam(required = false) Set<TransferStatus> status,
                                                        @RequestParam(required = false) String q,
                                                        Pageable pageable) {
        userContext.requireWarehouseAccess(warehouseId);
        return service.search(new TransferFilter(warehouseId, status, q), pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(InventoryAccess.ANY_USER)
    public TransferResponse get(@PathVariable UUID id) {
        requireAnyAccess(service.accessInfo(id));
        return service.getById(id);
    }

    @PostMapping("/{id}/ship")
    @PreAuthorize(InventoryAccess.MANAGER)
    public TransferResponse ship(@PathVariable UUID id) {
        userContext.requireWarehouseAccess(service.accessInfo(id).sourceWarehouseId());
        return service.ship(id);
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize(InventoryAccess.WORKER)
    public TransferResponse receive(@PathVariable UUID id, @RequestBody ReceiveTransferRequest request) {
        userContext.requireWarehouseAccess(service.accessInfo(id).destinationWarehouseId());
        return service.receive(id, request);
    }

    @PostMapping("/{id}/lines/{lineId}/confirm")
    @PreAuthorize(InventoryAccess.WORKER)
    public TransferResponse confirmLine(@PathVariable UUID id, @PathVariable UUID lineId,
                                        @RequestBody ConfirmTransferLineRequest request) {
        userContext.requireWarehouseAccess(service.accessInfo(id).sourceWarehouseId());
        return service.confirmLine(id, lineId, request, userContext.currentUserId(), isManager());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(InventoryAccess.MANAGER)
    public TransferResponse cancel(@PathVariable UUID id, @RequestBody CancelTransferRequest request) {
        requireAnyAccess(service.accessInfo(id));
        return service.cancel(id, request.reason());
    }

    private boolean isManager() {
        return userContext.isAdmin() || userContext.hasRole("WAREHOUSE_MANAGER");
    }

    /** A transfer is visible to users of either warehouse. */
    private void requireAnyAccess(TransferAccessInfo info) {
        if (!userContext.canAccessWarehouse(info.sourceWarehouseId())
                && !userContext.canAccessWarehouse(info.destinationWarehouseId())) {
            throw new ForbiddenException("No access to this transfer");
        }
    }
}
