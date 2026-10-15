package rw.afriteck.wms.inbound.dtos;

import rw.afriteck.wms.inbound.enums.ReceiptMode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GoodsReceiptResponse(
        UUID id, long version, String receiptNumber,
        UUID purchaseOrderId, String poNumber,
        UUID warehouseId,
        ReceiptMode mode,
        Instant receivedAt, UUID receivedBy, String receivedByName,
        String deliveryNoteNumber, String notes,
        UUID taskId,                               // PUTAWAY task in operations; null for DIRECT
        List<GoodsReceiptLineResponse> lines) {}