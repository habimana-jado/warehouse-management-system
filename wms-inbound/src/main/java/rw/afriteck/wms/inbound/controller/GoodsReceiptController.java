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
import rw.afriteck.wms.inbound.dtos.ConfirmPutawayRequest;
import rw.afriteck.wms.inbound.dtos.GoodsReceiptResponse;
import rw.afriteck.wms.inbound.dtos.GoodsReceiptSummaryResponse;
import rw.afriteck.wms.inbound.dtos.ReceiveGoodsRequest;
import rw.afriteck.wms.inbound.service.GoodsReceiptService;
import rw.afriteck.wms.inbound.service.PurchaseOrderService;

import java.util.List;
import java.util.UUID;

/** Mapped at /inbound because receiving lives under a purchase order while reading and put-away live under /goods-receipts. */
@RestController
@RequestMapping("/api/v1/wms/inbound")
@RequiredArgsConstructor
public class GoodsReceiptController {

    private final GoodsReceiptService receipts;
    private final PurchaseOrderService purchaseOrders;
    private final UserContext userContext;

    // ---- under a purchase order

    @PostMapping("/purchase-orders/{purchaseOrderId}/receipts")
    @PreAuthorize(InboundAccess.RECEIVE)
    public ResponseEntity<GoodsReceiptResponse> receive(@PathVariable UUID purchaseOrderId,
                                                        @Valid @RequestBody ReceiveGoodsRequest request) {
        userContext.requireWarehouseAccess(purchaseOrders.warehouseIdOf(purchaseOrderId));
        GoodsReceiptResponse response = receipts.receive(purchaseOrderId, request, userContext.currentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/purchase-orders/{purchaseOrderId}/receipts")
    @PreAuthorize(InboundAccess.READ)
    public List<GoodsReceiptSummaryResponse> listForPurchaseOrder(@PathVariable UUID purchaseOrderId) {
        userContext.requireWarehouseAccess(purchaseOrders.warehouseIdOf(purchaseOrderId));
        return receipts.findByPurchaseOrder(purchaseOrderId);
    }

    // ---- receipts themselves

    @GetMapping("/goods-receipts")
    @PreAuthorize(InboundAccess.READ)
    public PageResponse<GoodsReceiptSummaryResponse> search(@RequestParam UUID warehouseId,
                                                            @RequestParam(required = false) String q,
                                                            Pageable pageable) {
        userContext.requireWarehouseAccess(warehouseId);
        return receipts.search(warehouseId, q, pageable);
    }

    /** The put-away board: receipts that still have lines waiting to be moved to storage. */
    @GetMapping("/goods-receipts/awaiting-putaway")
    @PreAuthorize(InboundAccess.READ)
    public PageResponse<GoodsReceiptSummaryResponse> awaitingPutaway(@RequestParam UUID warehouseId,
                                                                     Pageable pageable) {
        userContext.requireWarehouseAccess(warehouseId);
        return receipts.awaitingPutaway(warehouseId, pageable);
    }

    @GetMapping("/goods-receipts/{id}")
    @PreAuthorize(InboundAccess.READ)
    public GoodsReceiptResponse get(@PathVariable UUID id) {
        GoodsReceiptResponse response = receipts.getById(id);
        userContext.requireWarehouseAccess(response.warehouseId());
        return response;
    }

    /**
     * Worker confirms one put-away line. The task line's referenceId is the receipt id and its referenceLineId is
     * the receipt line id. An empty body means "the full quantity into the suggested bin".
     */
    @PostMapping("/goods-receipts/{id}/lines/{lineId}/putaway")
    @PreAuthorize(InboundAccess.WORKER)
    public GoodsReceiptResponse confirmPutaway(@PathVariable UUID id, @PathVariable UUID lineId,
                                               @Valid @RequestBody(required = false) ConfirmPutawayRequest request) {
        userContext.requireWarehouseAccess(receipts.warehouseIdOf(id));
        boolean privileged = userContext.isAdmin() || userContext.hasRole("WAREHOUSE_MANAGER");
        return receipts.confirmPutaway(id, lineId, request, userContext.currentUserId(), privileged);
    }
}