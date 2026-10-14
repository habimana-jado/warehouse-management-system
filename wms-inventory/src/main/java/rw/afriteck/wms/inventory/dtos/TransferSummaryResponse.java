package rw.afriteck.wms.inventory.dtos;

import rw.afriteck.wms.inventory.enums.TransferStatus;
import rw.afriteck.wms.inventory.enums.TransferType;

import java.time.Instant;
import java.util.UUID;

public record TransferSummaryResponse(
        UUID id,
        String transferNumber,
        TransferType type,
        TransferStatus status,
        UUID sourceWarehouseId,
        UUID destinationWarehouseId,
        int lineCount,
        Instant shippedAt,
        Instant completedAt,
        Instant createdAt) {}
