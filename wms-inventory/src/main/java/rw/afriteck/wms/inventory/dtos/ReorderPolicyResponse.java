package rw.afriteck.wms.inventory.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record ReorderPolicyResponse(
        UUID id, long version, UUID warehouseId,
        UUID itemId, String itemSku, String itemName, String baseUom,
        BigDecimal minQty, BigDecimal reorderQty, BigDecimal maxQty, boolean active) {}
