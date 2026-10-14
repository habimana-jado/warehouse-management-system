package rw.afriteck.wms.inventory.dtos;

import rw.afriteck.wms.inventory.enums.AdjustmentDirection;

public record CreateAdjustmentReasonRequest(
        String code, String name,
        AdjustmentDirection direction,          // null -> BOTH
        Boolean requiresApproval               // null -> true
) {}