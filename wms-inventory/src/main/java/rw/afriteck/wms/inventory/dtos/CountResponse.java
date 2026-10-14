package rw.afriteck.wms.inventory.dtos;

import rw.afriteck.wms.inventory.enums.CountStatus;

import java.time.Instant;
import java.util.UUID;

public record CountResponse(
        UUID id, long version,
        String countNumber,
        UUID warehouseId, UUID zoneLocationId,
        CountStatus status, Instant startedAt, Instant completedAt, UUID approvedBy, String approvedByName,
        String cancelReason, String notes, Instant createdAt,
        long totalLines, long countedLines, Long varianceLines) {}           // varianceLines is null in list results
