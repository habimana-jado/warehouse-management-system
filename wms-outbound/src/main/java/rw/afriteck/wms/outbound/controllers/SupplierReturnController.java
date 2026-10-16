package rw.afriteck.wms.outbound.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.outbound.dtos.CancelSupplierReturnRequest;
import rw.afriteck.wms.outbound.dtos.CreateSupplierReturnRequest;
import rw.afriteck.wms.outbound.dtos.SupplierReturnResponse;
import rw.afriteck.wms.outbound.dtos.SupplierReturnSummaryResponse;
import rw.afriteck.wms.outbound.enums.SupplierReturnStatus;
import rw.afriteck.wms.outbound.service.SupplierReturnService;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wms/outbound/supplier-returns")
@RequiredArgsConstructor
public class SupplierReturnController {

    private final SupplierReturnService returns;
    private final UserContext userContext;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(OutboundAccess.MANAGER)
    public SupplierReturnResponse create(@RequestBody CreateSupplierReturnRequest request) {
        if (request.warehouseId() != null) userContext.requireWarehouseAccess(request.warehouseId());
        return returns.create(request);
    }

    @GetMapping
    @PreAuthorize(OutboundAccess.READ)
    public PageResponse<SupplierReturnSummaryResponse> search(@RequestParam UUID warehouseId,
                                                              @RequestParam(required = false) Set<SupplierReturnStatus> statuses,
                                                              @RequestParam(required = false) String q,
                                                              Pageable pageable) {
        userContext.requireWarehouseAccess(warehouseId);
        return returns.search(warehouseId, statuses, q, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(OutboundAccess.READ)
    public SupplierReturnResponse get(@PathVariable UUID id) {
        userContext.requireWarehouseAccess(returns.warehouseIdOf(id));
        return returns.getById(id);
    }

    @PostMapping("/{id}/post")
    @PreAuthorize(OutboundAccess.MANAGER)
    public SupplierReturnResponse post(@PathVariable UUID id) {
        userContext.requireWarehouseAccess(returns.warehouseIdOf(id));
        return returns.post(id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(OutboundAccess.MANAGER)
    public SupplierReturnResponse cancel(@PathVariable UUID id, @RequestBody CancelSupplierReturnRequest request) {
        userContext.requireWarehouseAccess(returns.warehouseIdOf(id));
        return returns.cancel(id, request);
    }
}
