package rw.afriteck.wms.inventory.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateReorderPolicyRequest(
        UUID warehouseId, UUID itemId,
        BigDecimal minQty,                // reorder point, >= 0, base UOM
        BigDecimal reorderQty,            // suggested order size, > 0
        BigDecimal maxQty) {}             // optional, must be above minQty
