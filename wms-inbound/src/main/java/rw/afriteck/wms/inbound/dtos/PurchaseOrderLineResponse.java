package rw.afriteck.wms.inbound.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseOrderLineResponse(
        UUID id, int lineNo,
        UUID itemId, String itemSku, String itemName, String baseUom,
        BigDecimal orderedQty, BigDecimal acceptedQty, BigDecimal rejectedQty,
        BigDecimal remainingQty,                   // still needed to fulfil the order
        BigDecimal unitPrice,                      // per base unit, null = unknown
        BigDecimal tolerancePercent,
        BigDecimal acceptableQtyLeft,              // what may still be accepted, tolerance included
        BigDecimal lineTotal,                      // orderedQty x unitPrice, null when the price is unknown
        String notes) {}