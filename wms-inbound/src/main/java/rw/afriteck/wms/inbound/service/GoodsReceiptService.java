package rw.afriteck.wms.inbound.service;

import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.inbound.dtos.ConfirmPutawayRequest;
import rw.afriteck.wms.inbound.dtos.GoodsReceiptResponse;
import rw.afriteck.wms.inbound.dtos.GoodsReceiptSummaryResponse;
import rw.afriteck.wms.inbound.dtos.ReceiveGoodsRequest;

import java.util.List;
import java.util.UUID;

public interface GoodsReceiptService {

    /** Receives goods against a PO: posts stock, updates the PO, creates the PUTAWAY task (PUTAWAY mode). */
    GoodsReceiptResponse receive(UUID purchaseOrderId, ReceiveGoodsRequest request, UUID actorId);

    /**
     * Confirms one put-away line: posts the move, then completes the task line.
     * Non-privileged callers must be the task's assignee (403 otherwise).
     */
    GoodsReceiptResponse confirmPutaway(UUID receiptId, UUID lineId, ConfirmPutawayRequest request,
                                        UUID actorId, boolean privileged);

    GoodsReceiptResponse getById(UUID id);

    List<GoodsReceiptSummaryResponse> findByPurchaseOrder(UUID purchaseOrderId);

    /** q (optional) matches the receipt number or the delivery note number. */
    PageResponse<GoodsReceiptSummaryResponse> search(UUID warehouseId, String q, Pageable pageable);

    /** Receipts with at least one line still waiting for put-away. */
    PageResponse<GoodsReceiptSummaryResponse> awaitingPutaway(UUID warehouseId, Pageable pageable);

    /** For controllers' warehouse access checks. 404 when the receipt does not exist. */
    UUID warehouseIdOf(UUID receiptId);
}