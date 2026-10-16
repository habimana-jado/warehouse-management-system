package rw.afriteck.wms.outbound.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.outbound.dtos.*;
import rw.afriteck.wms.outbound.enums.SalesOrderStatus;
import rw.afriteck.wms.outbound.service.PickListService;
import rw.afriteck.wms.outbound.service.SalesOrderService;
import rw.afriteck.wms.outbound.service.ShipmentService;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wms/outbound/sales-orders")
@RequiredArgsConstructor
public class SalesOrderController {

    private final SalesOrderService salesOrders;
    private final PickListService pickLists;
    private final ShipmentService shipments;
    private final UserContext userContext;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(OutboundAccess.MANAGER)
    public SalesOrderResponse create(@RequestBody CreateSalesOrderRequest request) {
        if (request.warehouseId() != null) userContext.requireWarehouseAccess(request.warehouseId());   // null: the service rejects it
        return salesOrders.create(request);
    }

    @GetMapping
    @PreAuthorize(OutboundAccess.READ)
    public PageResponse<SalesOrderSummaryResponse> search(
            @RequestParam UUID warehouseId,
            @RequestParam(required = false) Set<SalesOrderStatus> statuses,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID itemId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate orderDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate orderDateTo,
            @RequestParam(required = false) String q,
            Pageable pageable) {
        userContext.requireWarehouseAccess(warehouseId);
        return salesOrders.search(new SalesOrderFilter(warehouseId, statuses, customerId, itemId,
                orderDateFrom, orderDateTo, q), pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(OutboundAccess.READ)
    public SalesOrderResponse get(@PathVariable UUID id) {
        userContext.requireWarehouseAccess(salesOrders.warehouseIdOf(id));
        return salesOrders.getById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize(OutboundAccess.MANAGER)
    public SalesOrderResponse update(@PathVariable UUID id, @RequestBody UpdateSalesOrderRequest request) {
        userContext.requireWarehouseAccess(salesOrders.warehouseIdOf(id));
        return salesOrders.update(id, request);
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize(OutboundAccess.MANAGER)
    public ConfirmSalesOrderResponse confirm(@PathVariable UUID id) {
        userContext.requireWarehouseAccess(salesOrders.warehouseIdOf(id));
        return salesOrders.confirm(id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(OutboundAccess.MANAGER)
    public SalesOrderResponse cancel(@PathVariable UUID id, @RequestBody CancelSalesOrderRequest request) {
        userContext.requireWarehouseAccess(salesOrders.warehouseIdOf(id));
        return salesOrders.cancel(id, request);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize(OutboundAccess.MANAGER)
    public SalesOrderResponse close(@PathVariable UUID id) {
        userContext.requireWarehouseAccess(salesOrders.warehouseIdOf(id));
        return salesOrders.close(id);
    }

    // ---- nested: release for picking, pick lists, shipments ----

    @PostMapping("/{id}/release")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(OutboundAccess.MANAGER)
    public PickListResponse release(@PathVariable UUID id, @RequestBody(required = false) ReleaseSalesOrderRequest request) {
        userContext.requireWarehouseAccess(salesOrders.warehouseIdOf(id));
        return pickLists.release(id, request);                      // the body is optional: null means defaults
    }

    @GetMapping("/{id}/pick-lists")
    @PreAuthorize(OutboundAccess.READ)
    public List<PickListResponse> pickLists(@PathVariable UUID id) {
        userContext.requireWarehouseAccess(salesOrders.warehouseIdOf(id));
        return pickLists.findBySalesOrder(id);
    }

    @PostMapping("/{id}/shipments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(OutboundAccess.MANAGER)
    public ShipmentResponse ship(@PathVariable UUID id, @RequestBody(required = false) CreateShipmentRequest request) {
        userContext.requireWarehouseAccess(salesOrders.warehouseIdOf(id));
        return shipments.create(id, request);                       // null body = ship everything ready, no extra data
    }

    @GetMapping("/{id}/shipments")
    @PreAuthorize(OutboundAccess.READ)
    public List<ShipmentSummaryResponse> shipments(@PathVariable UUID id) {
        userContext.requireWarehouseAccess(salesOrders.warehouseIdOf(id));
        return shipments.findBySalesOrder(id);
    }
}
