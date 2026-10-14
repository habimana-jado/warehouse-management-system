package rw.afriteck.wms.inventory.dtos;

import java.util.UUID;

public record AdjustmentAccessInfo(UUID warehouseId, UUID requestedBy) {}     // used by controllers, like TaskAccessInfo
