package rw.afriteck.wms.outbound.dtos;

import rw.afriteck.wms.outbound.enums.ShipmentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShipmentResponse(UUID id, long version, String shipmentNumber,
                               UUID salesOrderId, String orderNumber, UUID customerId, String customerName, UUID warehouseId,
                               ShipmentStatus status, Instant shippedAt, String shipToAddress, String carrier, String trackingReference,
                               Integer packageCount, String notes, Instant deliveredAt, String receivedByName, String podNotes,
                               Instant createdAt, List<ShipmentLineResponse> lines) {}
