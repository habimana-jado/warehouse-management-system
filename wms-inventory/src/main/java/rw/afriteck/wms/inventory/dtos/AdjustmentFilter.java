package rw.afriteck.wms.inventory.dtos;

import rw.afriteck.wms.inventory.enums.AdjustmentStatus;

import java.util.Set;
import java.util.UUID;

public record AdjustmentFilter(
        UUID warehouseId,
        Set<AdjustmentStatus> statuses,
        String q, UUID requestedBy          // q: number or notes, requestedBy null = all adjustments of the warehouse; set = only that requester's
) {
}


