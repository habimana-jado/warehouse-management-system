package rw.afriteck.wms.inventory.dtos;

import java.math.BigDecimal;

public record UpdateReorderPolicyRequest(
        BigDecimal minQty, BigDecimal reorderQty, BigDecimal maxQty,
        Boolean clearMaxQty,              // true removes the maximum; maxQty null alone means "unchanged"
        Boolean active) {}                // warehouse and item are immutable
