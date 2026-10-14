package rw.afriteck.wms.inventory.dtos;

import rw.afriteck.wms.inventory.enums.TransferStatus;
import rw.afriteck.wms.inventory.enums.TransferType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        long version,
        String transferNumber,
        TransferType type,
        TransferStatus status,
        UUID sourceWarehouseId,
        UUID destinationWarehouseId,
        Instant shippedAt,
        Instant completedAt,
        String cancelReason,
        String notes,
        Instant createdAt,
        List<TransferLineResponse> lines) {
}

