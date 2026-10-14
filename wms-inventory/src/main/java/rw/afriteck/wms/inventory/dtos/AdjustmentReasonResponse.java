package rw.afriteck.wms.inventory.dtos;

import rw.afriteck.wms.inventory.enums.AdjustmentDirection;

import java.util.UUID;

public record AdjustmentReasonResponse(
        UUID id, long version, String code, String name,
        AdjustmentDirection direction, boolean requiresApproval, boolean active) {}
