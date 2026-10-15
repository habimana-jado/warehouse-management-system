package rw.afriteck.wms.inbound.dtos;

import rw.afriteck.wms.inbound.enums.ReceiptMode;

import java.time.Instant;
import java.util.UUID;

public record GoodsReceiptSummaryResponse(
        UUID id, String receiptNumber,
        UUID purchaseOrderId, String poNumber,
        UUID warehouseId,
        ReceiptMode mode,
        Instant receivedAt, UUID receivedBy, String receivedByName,
        String deliveryNoteNumber,
        int lineCount, int pendingPutawayCount) {}