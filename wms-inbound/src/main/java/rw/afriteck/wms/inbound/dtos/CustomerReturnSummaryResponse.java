package rw.afriteck.wms.inbound.dtos;

import rw.afriteck.wms.inbound.enums.CustomerReturnStatus;

import java.time.Instant;
import java.util.UUID;

public record CustomerReturnSummaryResponse(
        UUID id, String returnNumber,
        UUID warehouseId,
        UUID customerId, String customerName,
        String salesOrderNumber,
        CustomerReturnStatus status,
        Instant createdAt, Instant receivedAt,
        int lineCount) {}