package rw.afriteck.wms.inventory.dtos;

import rw.afriteck.wms.inventory.enums.AdjustmentStatus;

import java.time.Instant;
import java.util.UUID;

public record AdjustmentSummaryResponse(
        UUID id, String adjustmentNumber, UUID warehouseId,
        UUID reasonId, String reasonCode, String reasonName, AdjustmentStatus status,
        UUID requestedBy, String requestedByName, int lineCount, Instant createdAt) {
}
