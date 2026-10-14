package rw.afriteck.wms.inventory.dtos;

import rw.afriteck.wms.inventory.enums.AdjustmentDirection;

public record UpdateAdjustmentReasonRequest(
        String name,
        AdjustmentDirection direction,
        Boolean requiresApproval,
        Boolean active) {}      // null field = unchanged; code is immutable

