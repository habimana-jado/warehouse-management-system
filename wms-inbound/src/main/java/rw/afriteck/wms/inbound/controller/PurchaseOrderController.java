package rw.afriteck.wms.inbound.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.inbound.dtos.*;
import rw.afriteck.wms.inbound.enums.PurchaseOrderStatus;
import rw.afriteck.wms.inbound.service.PurchaseOrderService;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wms/inbound/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrders;
    private final UserContext userContext;

    @PostMapping
    @PreAuthorize(InboundAccess.MANAGER)
    public ResponseEntity<PurchaseOrderResponse> create(@Valid @RequestBody CreatePurchaseOrderRequest request) {
        userContext.requireWarehouseAccess(request.warehouseId());
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseOrders.create(request));
    }

    @GetMapping
    @PreAuthorize(InboundAccess.READ)
    public PageResponse<PurchaseOrderSummaryResponse> search(
            @RequestParam UUID warehouseId,
            @RequestParam(required = false) Set<PurchaseOrderStatus> statuses,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) UUID itemId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate orderDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate orderDateTo,
            @RequestParam(required = false) String q,
            Pageable pageable) {
        userContext.requireWarehouseAccess(warehouseId);
        PurchaseOrderFilter filter = new PurchaseOrderFilter(
                warehouseId, statuses, supplierId, itemId, orderDateFrom, orderDateTo, q);
        return purchaseOrders.search(filter, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(InboundAccess.READ)
    public PurchaseOrderResponse get(@PathVariable UUID id) {
        PurchaseOrderResponse response = purchaseOrders.getById(id);
        userContext.requireWarehouseAccess(response.warehouseId());
        return response;
    }

    @PutMapping("/{id}")
    @PreAuthorize(InboundAccess.MANAGER)
    public PurchaseOrderResponse update(@PathVariable UUID id, @Valid @RequestBody UpdatePurchaseOrderRequest request) {
        requireAccess(id);
        return purchaseOrders.update(id, request);
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize(InboundAccess.MANAGER)
    public PurchaseOrderResponse confirm(@PathVariable UUID id) {
        requireAccess(id);
        return purchaseOrders.confirm(id, userContext.currentUserId());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(InboundAccess.MANAGER)
    public PurchaseOrderResponse cancel(@PathVariable UUID id, @Valid @RequestBody CancelPurchaseOrderRequest request) {
        requireAccess(id);
        return purchaseOrders.cancel(id, request.reason());
    }

    /** "Close short": ends a partially received PO whose remaining quantity will not arrive. */
    @PostMapping("/{id}/close")
    @PreAuthorize(InboundAccess.MANAGER)
    public PurchaseOrderResponse close(@PathVariable UUID id) {
        requireAccess(id);
        return purchaseOrders.close(id, userContext.currentUserId());
    }

    @PatchMapping("/{id}/lines/{lineId}/tolerance")
    @PreAuthorize(InboundAccess.MANAGER)
    public PurchaseOrderResponse changeTolerance(@PathVariable UUID id, @PathVariable UUID lineId,
                                                 @Valid @RequestBody ChangeToleranceRequest request) {
        requireAccess(id);
        return purchaseOrders.changeTolerance(id, lineId, request.tolerancePercent());
    }

    /** Receiver's handheld: item or box barcode -> the PO line, with the scanned unit and its factor to base. */
    @GetMapping("/{id}/scan/{barcode}")
    @PreAuthorize(InboundAccess.RECEIVE)
    public PurchaseOrderLineScanResponse scan(@PathVariable UUID id, @PathVariable String barcode) {
        requireAccess(id);
        return purchaseOrders.scan(id, barcode);
    }

    private void requireAccess(UUID purchaseOrderId) {
        userContext.requireWarehouseAccess(purchaseOrders.warehouseIdOf(purchaseOrderId));
    }
}