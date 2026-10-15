package rw.afriteck.wms.inbound.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.inbound.dtos.*;
import rw.afriteck.wms.inbound.enums.CustomerReturnStatus;
import rw.afriteck.wms.inbound.service.CustomerReturnService;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wms/inbound/customer-returns")
@RequiredArgsConstructor
public class CustomerReturnController {

    private final CustomerReturnService customerReturns;
    private final UserContext userContext;

    @PostMapping
    @PreAuthorize(InboundAccess.MANAGER)
    public ResponseEntity<CustomerReturnResponse> create(@Valid @RequestBody CreateCustomerReturnRequest request) {
        userContext.requireWarehouseAccess(request.warehouseId());
        return ResponseEntity.status(HttpStatus.CREATED).body(customerReturns.create(request));
    }

    @GetMapping
    @PreAuthorize(InboundAccess.READ)
    public PageResponse<CustomerReturnSummaryResponse> search(
            @RequestParam UUID warehouseId,
            @RequestParam(required = false) Set<CustomerReturnStatus> statuses,
            @RequestParam(required = false) String q,
            Pageable pageable) {
        userContext.requireWarehouseAccess(warehouseId);
        return customerReturns.search(warehouseId, statuses, q, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(InboundAccess.READ)
    public CustomerReturnResponse get(@PathVariable UUID id) {
        CustomerReturnResponse response = customerReturns.getById(id);
        userContext.requireWarehouseAccess(response.warehouseId());
        return response;
    }

    /** Goods arrived and were inspected: posts the stock and marks the return RECEIVED. */
    @PostMapping("/{id}/receive")
    @PreAuthorize(InboundAccess.RECEIVE)
    public CustomerReturnResponse receive(@PathVariable UUID id,
                                          @Valid @RequestBody ReceiveCustomerReturnRequest request) {
        userContext.requireWarehouseAccess(customerReturns.warehouseIdOf(id));
        return customerReturns.receive(id, request, userContext.currentUserId());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(InboundAccess.MANAGER)
    public CustomerReturnResponse cancel(@PathVariable UUID id,
                                         @Valid @RequestBody CancelCustomerReturnRequest request) {
        userContext.requireWarehouseAccess(customerReturns.warehouseIdOf(id));
        return customerReturns.cancel(id, request.reason());
    }
}