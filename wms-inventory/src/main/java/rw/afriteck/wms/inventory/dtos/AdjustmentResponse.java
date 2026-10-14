package rw.afriteck.wms.inventory.dtos;

import rw.afriteck.wms.inventory.enums.AdjustmentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdjustmentResponse(
        UUID id, long version, String adjustmentNumber, UUID warehouseId,
        UUID reasonId, String reasonCode, String reasonName, AdjustmentStatus status, String notes,
        UUID requestedBy, String requestedByName, UUID decidedBy, String decidedByName,
        Instant decidedAt, String rejectionReason, Instant createdAt,
        List<AdjustmentLineResponse> lines) {
}
