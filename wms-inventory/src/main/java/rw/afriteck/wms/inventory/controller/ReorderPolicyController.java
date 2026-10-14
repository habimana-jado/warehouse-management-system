package rw.afriteck.wms.inventory.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.inventory.dtos.CreateReorderPolicyRequest;
import rw.afriteck.wms.inventory.dtos.LowStockResponse;
import rw.afriteck.wms.inventory.dtos.ReorderPolicyResponse;
import rw.afriteck.wms.inventory.dtos.UpdateReorderPolicyRequest;
import rw.afriteck.wms.inventory.service.ReorderPolicyService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wms/inventory/reorder-policies")
@RequiredArgsConstructor
public class ReorderPolicyController {

    private final ReorderPolicyService service;
    private final UserContext userContext;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(InventoryAccess.MANAGER)
    public ReorderPolicyResponse create(@RequestBody CreateReorderPolicyRequest request) {
        userContext.requireWarehouseAccess(request.warehouseId());
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(InventoryAccess.MANAGER)
    public ReorderPolicyResponse update(@PathVariable UUID id, @RequestBody UpdateReorderPolicyRequest request) {
        userContext.requireWarehouseAccess(service.warehouseIdOf(id));
        return service.update(id, request);
    }

    @GetMapping
    @PreAuthorize(InventoryAccess.BOARD)
    public PageResponse<ReorderPolicyResponse> search(@RequestParam UUID warehouseId,
                                                      @RequestParam(defaultValue = "true") boolean activeOnly,
                                                      Pageable pageable) {
        userContext.requireWarehouseAccess(warehouseId);
        return service.search(warehouseId, activeOnly, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(InventoryAccess.BOARD)
    public ReorderPolicyResponse get(@PathVariable UUID id) {
        userContext.requireWarehouseAccess(service.warehouseIdOf(id));
        return service.getById(id);
    }

    @GetMapping("/low-stock")
    @PreAuthorize(InventoryAccess.BOARD)
    public List<LowStockResponse> lowStock(@RequestParam UUID warehouseId) {
        userContext.requireWarehouseAccess(warehouseId);
        return service.findBelowMinimum(warehouseId);
    }
}
