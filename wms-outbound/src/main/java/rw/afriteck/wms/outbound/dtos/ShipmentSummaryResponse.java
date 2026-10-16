package rw.afriteck.wms.outbound.dtos;

import rw.afriteck.wms.outbound.enums.ShipmentStatus;

import java.time.Instant;
import java.util.UUID;

public record ShipmentSummaryResponse(UUID id, String shipmentNumber, UUID salesOrderId, String orderNumber,
                                      UUID customerId, String customerName, UUID warehouseId, ShipmentStatus status,
                                      Instant shippedAt, Integer packageCount, int lineCount) {}
