package rw.afriteck.wms.outbound.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.outbound.dtos.ConfirmDeliveryRequest;
import rw.afriteck.wms.outbound.dtos.ShipmentResponse;
import rw.afriteck.wms.outbound.dtos.ShipmentSummaryResponse;
import rw.afriteck.wms.outbound.service.ShipmentService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wms/outbound/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipments;
    private final UserContext userContext;

    @GetMapping
    @PreAuthorize(OutboundAccess.READ)
    public PageResponse<ShipmentSummaryResponse> search(@RequestParam UUID warehouseId,
                                                        @RequestParam(required = false) String q,
                                                        Pageable pageable) {
        userContext.requireWarehouseAccess(warehouseId);
        return shipments.search(warehouseId, q, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(OutboundAccess.READ)
    public ShipmentResponse get(@PathVariable UUID id) {
        userContext.requireWarehouseAccess(shipments.warehouseIdOf(id));
        return shipments.getById(id);
    }

    @PostMapping("/{id}/delivery")
    @PreAuthorize(OutboundAccess.MANAGER)
    public ShipmentResponse confirmDelivery(@PathVariable UUID id,
                                            @RequestBody(required = false) ConfirmDeliveryRequest request) {
        userContext.requireWarehouseAccess(shipments.warehouseIdOf(id));
        return shipments.confirmDelivery(id, request);
    }
}
