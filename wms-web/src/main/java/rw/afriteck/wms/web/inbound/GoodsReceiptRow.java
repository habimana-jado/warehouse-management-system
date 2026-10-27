package rw.afriteck.wms.web.inbound;

import rw.afriteck.wms.inbound.dtos.GoodsReceiptSummaryResponse;
import rw.afriteck.wms.inbound.enums.ReceiptMode;

import java.time.Instant;
import java.util.UUID;

/** One row of the goods receipt list and of the put-away board. */
public record GoodsReceiptRow(UUID id, String receiptNumber, String poNumber, ReceiptMode mode,
                              Instant receivedAt, String receivedByName, String deliveryNote,
                              int lineCount, int pendingPutawayCount) {

    static GoodsReceiptRow of(GoodsReceiptSummaryResponse s) {
        return new GoodsReceiptRow(s.id(), s.receiptNumber(), s.poNumber(), s.mode(), s.receivedAt(),
                s.receivedByName(), s.deliveryNoteNumber(), s.lineCount(), s.pendingPutawayCount());
    }
}