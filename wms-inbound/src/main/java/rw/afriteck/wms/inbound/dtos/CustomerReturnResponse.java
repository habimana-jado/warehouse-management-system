package rw.afriteck.wms.inbound.dtos;

import rw.afriteck.wms.inbound.enums.CustomerReturnStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CustomerReturnResponse(
        UUID id, long version, String returnNumber,
        UUID warehouseId,
        UUID customerId, String customerName,
        UUID salesOrderId, String salesOrderNumber,
        CustomerReturnStatus status, String notes,
        Instant receivedAt, UUID receivedBy, String receivedByName,
        Instant cancelledAt, String cancelReason,
        Instant createdAt,
        List<CustomerReturnLineResponse> lines) {}