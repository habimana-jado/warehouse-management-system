package rw.afriteck.wms.web.inbound;

import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.inbound.dtos.PurchaseOrderLineResponse;

import java.math.BigDecimal;
import java.util.UUID;

/** What the receive form needs to know about one PO line. tracking is NONE, LOT or SERIAL (a plain string for the template). */
public record ReceiveLineInfo(UUID poLineId, int lineNo, String itemSku, String itemName, String baseUom,
                              String tracking, boolean expiryTracked,
                              BigDecimal orderedQty, BigDecimal remainingQty, BigDecimal acceptableLeft) {

    static ReceiveLineInfo of(PurchaseOrderLineResponse l, ItemResponse item) {
        return new ReceiveLineInfo(l.id(), l.lineNo(), l.itemSku(), l.itemName(), l.baseUom(),
                item == null ? "NONE" : item.trackingType().name(),
                item != null && item.expiryTracked(),
                l.orderedQty(), l.remainingQty(), l.acceptableQtyLeft());
    }
}