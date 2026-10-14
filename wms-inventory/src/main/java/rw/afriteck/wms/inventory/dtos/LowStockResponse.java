package rw.afriteck.wms.inventory.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record LowStockResponse(
        UUID itemId, String itemSku, String itemName, String baseUom,
        BigDecimal onHand,                // quantity in STORAGE bins
        BigDecimal minQty, BigDecimal reorderQty, BigDecimal maxQty,
        BigDecimal suggestedQty) {}
